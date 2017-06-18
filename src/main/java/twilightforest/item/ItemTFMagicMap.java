package twilightforest.item;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.MapColor;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketMaps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeRiver;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import twilightforest.TFFeature;
import twilightforest.TFPacketHandler;
import twilightforest.TFMagicMapData;
import twilightforest.TwilightForestMod;
import twilightforest.biomes.TFBiomeBase;
import twilightforest.biomes.TFBiomes;
import twilightforest.network.PacketMagicMapFeatures;
import twilightforest.network.PacketMapRewrap;
import twilightforest.world.TFBiomeProvider;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;

public class ItemTFMagicMap extends ItemMap
{
    private static final String STR_ID = "map";
    private static final HashMap<Biome, MapColorBrightness> BIOME_COLORS = new HashMap<>();

    private static class MapColorBrightness {
        public MapColor color;
        public int brightness;

        public MapColorBrightness (MapColor color, int brightness) {
            this.color = color;
            this.brightness = brightness;
        }

        public MapColorBrightness (MapColor color) {
            this.color = color;
            this.brightness = 1;
        }
    }

    // [VanillaCopy] super with own id
    public static ItemStack setupNewMap(World world, double worldX, double worldZ, byte scale, boolean trackingPosition, boolean unlimitedTracking)
    {
        // TF - own item and string id
        ItemStack itemstack = new ItemStack(TFItems.magicMap, 1, world.getUniqueDataId(ItemTFMagicMap.STR_ID));
        String s = ItemTFMagicMap.STR_ID + "_" + itemstack.getMetadata();
        MapData mapdata = new TFMagicMapData(s);
        world.setData(s, mapdata);
        mapdata.scale = scale;
        mapdata.calculateMapCenter(worldX, worldZ, mapdata.scale);
        mapdata.dimension = world.provider.getDimension();
        mapdata.trackingPosition = trackingPosition;
        mapdata.unlimitedTracking = unlimitedTracking;
        mapdata.markDirty();
        return itemstack;
    }

    // [VanillaCopy] super, with own string ID and class, narrowed types
    @SideOnly(Side.CLIENT)
    public static TFMagicMapData loadMapData(int mapId, World worldIn)
    {
        String s = STR_ID + "_" + mapId;
        TFMagicMapData mapdata = (TFMagicMapData)worldIn.loadData(TFMagicMapData.class, s);

        if (mapdata == null)
        {
            mapdata = new TFMagicMapData(s);
            worldIn.setData(s, mapdata);
        }

        return mapdata;
    }

    // [VanillaCopy] super, with own string ID and class
    @Override
	public TFMagicMapData getMapData(ItemStack stack, World worldIn)
    {
        String s = STR_ID + "_" + stack.getMetadata();
        TFMagicMapData mapdata = (TFMagicMapData) worldIn.loadData(TFMagicMapData.class, s);

        if (mapdata == null && !worldIn.isRemote)
        {
            stack.setItemDamage(worldIn.getUniqueDataId(STR_ID));
            s = STR_ID + "_" + stack.getMetadata();
            mapdata = new TFMagicMapData(s);
            mapdata.scale = 3;
            mapdata.calculateMapCenter((double)worldIn.getWorldInfo().getSpawnX(), (double)worldIn.getWorldInfo().getSpawnZ(), mapdata.scale);
            mapdata.dimension = worldIn.provider.getDimension();
            mapdata.markDirty();
            worldIn.setData(s, mapdata);
        }

        return mapdata;
    }

    // [VanillaCopy] of superclass, with sane variable names and noted changes
    @Override
    public void updateMapData(World world, Entity viewer, MapData data)
    {
        if (world.provider.getDimension() == data.dimension && viewer instanceof EntityPlayer)
        {
            int blocksPerPixel = 1 << data.scale;
            int centerX = data.xCenter;
            int centerZ = data.zCenter;
            int viewerX = MathHelper.floor(viewer.posX - (double)centerX) / blocksPerPixel + 64;
            int viewerZ = MathHelper.floor(viewer.posZ - (double)centerZ) / blocksPerPixel + 64;
            // TF - bump radius 128 -> 512
            int viewRadiusPixels = 512 / blocksPerPixel;

            if (world.provider.hasNoSky())
            {
                viewRadiusPixels /= 2;
            }

            MapData.MapInfo mapdata$mapinfo = data.getMapInfo((EntityPlayer)viewer);
            ++mapdata$mapinfo.step;
            boolean flag = false;

            for (int xPixel = viewerX - viewRadiusPixels + 1; xPixel < viewerX + viewRadiusPixels; ++xPixel)
            {
                if ((xPixel & 15) == (mapdata$mapinfo.step & 15) || flag)
                {
                    flag = false;
                    double d0 = 0.0D;

                    for (int zPixel = viewerZ - viewRadiusPixels - 1; zPixel < viewerZ + viewRadiusPixels; ++zPixel)
                    {
                        if (xPixel >= 0 && zPixel >= -1 && xPixel < 128 && zPixel < 128)
                        {
                            int xPixelDist = xPixel - viewerX;
                            int zPixelDist = zPixel - viewerZ;
                            boolean shouldFuzz = xPixelDist * xPixelDist + zPixelDist * zPixelDist > (viewRadiusPixels - 2) * (viewRadiusPixels - 2);
                            int worldX = (centerX / blocksPerPixel + xPixel - 64) * blocksPerPixel;
                            int worldZ = (centerZ / blocksPerPixel + zPixel - 64) * blocksPerPixel;
                            Multiset<MapColor> multiset = HashMultiset.<MapColor>create();

                            //TF: remove chunk loading and chunk empty check
                            // absolutely do not try to load every chunk on this map

                            int worldXRounded = worldX & 15;
                            int worldZRounded = worldZ & 15;
                            int numLiquid = 0;
                            double d1 = 0.0D;
                            int brightness = 1;

                            if (world.provider.hasNoSky())
                            {
                                int l3 = worldX + worldZ * 231871;
                                l3 = l3 * l3 * 31287121 + l3 * 11;

                                if ((l3 >> 20 & 1) == 0)
                                {
                                    multiset.add(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.DIRT).getMapColor(), 10);
                                }
                                else
                                {
                                    multiset.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.STONE).getMapColor(), 100);
                                }

                                d1 = 100.0D;
                            }
                            else
                            {
                                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                                for (int localX = 0; localX < blocksPerPixel; ++localX)
                                {
                                    for (int localZ = 0; localZ < blocksPerPixel; ++localZ)
                                    {
                                        // TF - inside of this loop replaced with biome color checks
                                        blockpos$mutableblockpos.setPos(localX + worldX, 0, localZ + worldZ);
                                        Biome biome = world.getBiome(blockpos$mutableblockpos);

                                        MapColorBrightness colorBrightness = this.getMapColorPerBiome(biome);
                                        multiset.add(colorBrightness.color, biome instanceof BiomeRiver || biome == TFBiomes.stream ? 3 : 1);
                                        brightness = colorBrightness.brightness;

                                        // add in TF features
                                        if (world.getBiomeProvider() instanceof TFBiomeProvider)
                                        {
                                            TFBiomeProvider tfManager  = (TFBiomeProvider) world.getBiomeProvider();

                                            if (TFFeature.isInFeatureChunk(world, worldXRounded + localX, worldZRounded + localZ) && zPixel >= 0 && xPixelDist * xPixelDist + zPixelDist * zPixelDist < viewRadiusPixels * viewRadiusPixels)
                                            {
                                                ((TFMagicMapData) data).addFeatureToMap(TFFeature.getNearestFeature((worldXRounded + localX) >> 4, (worldZRounded + localZ) >> 4, world), worldX, worldZ);
                                            }
                                        }
                                    }
                                }
                            }

                            numLiquid = numLiquid / (blocksPerPixel * blocksPerPixel);
                            double d2 = (d1 - d0) * 4.0D / (double)(blocksPerPixel + 4) + ((double)(xPixel + zPixel & 1) - 0.5D) * 0.4D;

                            if (d2 > 0.6D)
                            {
                                brightness = 2;
                            }

                            if (d2 < -0.6D)
                            {
                                brightness = 0;
                            }

                            MapColor mapcolor = (MapColor) Iterables.getFirst(Multisets.<MapColor>copyHighestCountFirst(multiset), MapColor.AIR);

                            if (mapcolor == MapColor.WATER)
                            {
                                d2 = (double)numLiquid * 0.1D + (double)(xPixel + zPixel & 1) * 0.2D;
                                brightness = 1;

                                if (d2 < 0.5D)
                                {
                                    brightness = 2;
                                }

                                if (d2 > 0.9D)
                                {
                                    brightness = 0;
                                }
                            }

                            d0 = d1;

                            if (zPixel >= 0 && xPixelDist * xPixelDist + zPixelDist * zPixelDist < viewRadiusPixels * viewRadiusPixels && (!shouldFuzz || (xPixel + zPixel & 1) != 0))
                            {
                                byte b0 = data.colors[xPixel + zPixel * 128];
                                byte b1 = (byte)(mapcolor.colorIndex * 4 + brightness);

                                if (b0 != b1)
                                {
                                    data.colors[xPixel + zPixel * 128] = b1;
                                    data.updateMapData(xPixel, zPixel);
                                    flag = true;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private MapColorBrightness getMapColorPerBiome(Biome biome)
    {
        if (BIOME_COLORS.isEmpty()) {
            ItemTFMagicMap.setupBiomeColors();
        }

        if (BIOME_COLORS.containsKey(biome)) {
            return BIOME_COLORS.get(biome);
        } else
        {
            return new MapColorBrightness(biome.topBlock.getMapColor());
        }
    }

    private static void setupBiomeColors()
    {
        BIOME_COLORS.put(TFBiomes.twilightForest, new MapColorBrightness(MapColor.FOLIAGE, 1));
        BIOME_COLORS.put(TFBiomes.denseTwilightForest, new MapColorBrightness(MapColor.FOLIAGE,0));
        BIOME_COLORS.put(TFBiomes.tfLake, new MapColorBrightness(MapColor.WATER, 3));
        BIOME_COLORS.put(TFBiomes.stream, new MapColorBrightness(MapColor.WATER, 1));
        BIOME_COLORS.put(TFBiomes.tfSwamp, new MapColorBrightness(MapColor.DIAMOND, 3));
        BIOME_COLORS.put(TFBiomes.fireSwamp, new MapColorBrightness(MapColor.NETHERRACK, 1));
        BIOME_COLORS.put(TFBiomes.clearing, new MapColorBrightness(MapColor.GRASS, 2));
        BIOME_COLORS.put(TFBiomes.oakSavanna, new MapColorBrightness(MapColor.GRASS, 0));
        BIOME_COLORS.put(TFBiomes.highlands, new MapColorBrightness(MapColor.DIRT, 0));
        BIOME_COLORS.put(TFBiomes.thornlands, new MapColorBrightness(MapColor.WOOD, 1));
        BIOME_COLORS.put(TFBiomes.highlandsCenter, new MapColorBrightness(MapColor.SILVER, 2));
        BIOME_COLORS.put(TFBiomes.fireflyForest, new MapColorBrightness(MapColor.EMERALD, 1));
        BIOME_COLORS.put(TFBiomes.darkForest, new MapColorBrightness(MapColor.GREEN, 3));
        BIOME_COLORS.put(TFBiomes.darkForestCenter, new MapColorBrightness(MapColor.ADOBE, 3));
        BIOME_COLORS.put(TFBiomes.snowy_forest, new MapColorBrightness(MapColor.SNOW, 1));
        BIOME_COLORS.put(TFBiomes.glacier, new MapColorBrightness(MapColor.ICE, 1));
        BIOME_COLORS.put(TFBiomes.mushrooms, new MapColorBrightness(MapColor.ADOBE, 0));
        BIOME_COLORS.put(TFBiomes.deepMushrooms, new MapColorBrightness(MapColor.PINK, 0));
    }

    @Override
	public void onCreated(ItemStack par1ItemStack, World par2World, EntityPlayer par3EntityPlayer)
    {
    	// disable zooming
    }
    
    @Override
	public EnumRarity getRarity(ItemStack par1ItemStack) {
    	return EnumRarity.UNCOMMON;
	}

    @Nullable
    public Packet<?> createMapDataPacket(ItemStack stack, World world, EntityPlayer player)
    {
        // Every so often, send a feature update packet instead of vanilla packet
        if (world.rand.nextInt(4) == 0) {
            TFMagicMapData data = this.getMapData(stack, world);
            data.checkExistingFeatures(world);

            IMessage packet = new PacketMagicMapFeatures(stack.getItemDamage(), data.serializeFeatures());
            return TFPacketHandler.CHANNEL.getPacketFrom(packet);
        } else {
            Packet<?> p = super.createMapDataPacket(stack, world, player);
            if (p instanceof SPacketMaps) {
                return TFPacketHandler.CHANNEL.getPacketFrom(new PacketMapRewrap(false, (SPacketMaps) p));
            } else {
                return p;
            }
        }
    }

    @Override
	public String getItemStackDisplayName(ItemStack par1ItemStack)
	{
		return ("" + I18n.format(this.getUnlocalizedNameInefficiently(par1ItemStack) + ".name") + " #" + par1ItemStack.getItemDamage()).trim();
    }
}
