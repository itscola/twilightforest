package twilightforest.block;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import twilightforest.TFSounds;
import twilightforest.advancements.TFAdvancements;
import twilightforest.entity.projectile.CicadaShot;
import twilightforest.entity.projectile.MoonwormShot;
import twilightforest.util.TFStats;

import javax.annotation.Nullable;

public abstract class CritterBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
	public static final DirectionProperty FACING = DirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private final VoxelShape DOWN_BB = Shapes.create(new AABB(0.2F, 0.85F, 0.2F, 0.8F, 1.0F, 0.8F));
	private final VoxelShape UP_BB = Shapes.create(new AABB(0.2F, 0.0F, 0.2F, 0.8F, 0.15F, 0.8F));
	private final VoxelShape NORTH_BB = Shapes.create(new AABB(0.2F, 0.2F, 0.85F, 0.8F, 0.8F, 1.0F));
	private final VoxelShape SOUTH_BB = Shapes.create(new AABB(0.2F, 0.2F, 0.0F, 0.8F, 0.8F, 0.15F));
	private final VoxelShape WEST_BB = Shapes.create(new AABB(0.85F, 0.2F, 0.2F, 1.0F, 0.8F, 0.8F));
	private final VoxelShape EAST_BB = Shapes.create(new AABB(0.0F, 0.2F, 0.2F, 0.15F, 0.8F, 0.8F));

	protected CritterBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.UP).setValue(WATERLOGGED, Boolean.FALSE));
	}

	@Override
	@Deprecated
	public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case DOWN -> DOWN_BB;
			default -> UP_BB;
			case NORTH -> NORTH_BB;
			case SOUTH -> SOUTH_BB;
			case WEST -> WEST_BB;
			case EAST -> EAST_BB;
		};
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction clicked = context.getClickedFace();
		FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
		BlockState state = defaultBlockState().setValue(FACING, clicked).setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);

		if (canSurvive(state, context.getLevel(), context.getClickedPos())) {
			return state;
		}

		for (Direction dir : context.getNearestLookingDirections()) {
			state = defaultBlockState().setValue(FACING, dir.getOpposite());
			if (canSurvive(state, context.getLevel(), context.getClickedPos())) {
				return state;
			}
		}
		return null;
	}

	@Override
	@Deprecated
	public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED)) {
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		}
		if (!canSurvive(state, level, pos)) {
			return Blocks.AIR.defaultBlockState();
		} else {
			return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
		}
	}

	@Override
	@Deprecated
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction facing = state.getValue(DirectionalBlock.FACING);
		BlockPos restingPos = pos.relative(facing.getOpposite());
		return canSupportCenter(level, restingPos, facing);
	}

	public abstract ItemStack getSquishResult(); // oh no! TODO Return Loot Table instead?

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() == Items.GLASS_BOTTLE) {
			if (this == TFBlocks.FIREFLY.get()) {
				if (!player.isCreative()) stack.shrink(1);
				player.getInventory().add(new ItemStack(TFBlocks.FIREFLY_JAR.get()));
				level.setBlockAndUpdate(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
				return InteractionResult.SUCCESS;
			} else if (this == TFBlocks.CICADA.get()) {
				if (!player.isCreative()) stack.shrink(1);
				player.getInventory().add(new ItemStack(TFBlocks.CICADA_JAR.get()));
				if (level.isClientSide())
					Minecraft.getInstance().getSoundManager().stop(TFSounds.CICADA.getLocation(), SoundSource.NEUTRAL);
				level.setBlockAndUpdate(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
				return InteractionResult.SUCCESS;
			}
		}
		return InteractionResult.PASS;
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if ((entity instanceof Projectile && !(entity instanceof MoonwormShot) && !(entity instanceof CicadaShot)) || entity instanceof FallingBlockEntity) {
			level.setBlockAndUpdate(pos, state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
			if (level.isClientSide())
				Minecraft.getInstance().getSoundManager().stop(TFSounds.CICADA.getLocation(), SoundSource.NEUTRAL);
			level.playSound(null, pos, TFSounds.BUG_SQUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
			ItemEntity squish = new ItemEntity(level, pos.getX(), pos.getY(), pos.getZ(), this.getSquishResult());
			squish.spawnAtLocation(squish.getItem());
			for (int i = 0; i < 50; i++) {
				boolean wallBug = state.getValue(FACING) != Direction.UP;
				level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState()), true,
						pos.getX() + Mth.nextFloat(level.getRandom(), 0.25F, 0.75F),
						pos.getY() + (wallBug ? 0.5F : 0.0F),
						pos.getZ() + Mth.nextFloat(level.getRandom(), 0.25F, 0.75F),
						0.0D, 0.0D, 0.0D);
			}
			if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
				player.awardStat(TFStats.BUGS_SQUISHED.get());
				TFAdvancements.KILL_BUG.trigger(player, state);
			}
		}
	}

	@Nullable
	@Override
	public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING, WATERLOGGED);
	}

}
