package twilightforest.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import twilightforest.capabilities.CapabilityList;
import twilightforest.capabilities.shield.IShieldCapability;

import java.util.function.Supplier;

public class UpdateShieldPacket {

	private final int entityID;
	private final int temporaryShields;
	private final int permanentShields;

	public UpdateShieldPacket(int id, IShieldCapability cap) {
		entityID = id;
		temporaryShields = cap.temporaryShieldsLeft();
		permanentShields = cap.permanentShieldsLeft();
	}

	public UpdateShieldPacket(Entity entity, IShieldCapability cap) {
		this(entity.getId(), cap);
	}

	public UpdateShieldPacket(FriendlyByteBuf buf) {
		entityID = buf.readInt();
		temporaryShields = buf.readInt();
		permanentShields = buf.readInt();
	}

	public void encode(FriendlyByteBuf buf) {
		buf.writeInt(entityID);
		buf.writeInt(temporaryShields);
		buf.writeInt(permanentShields);
	}

	public static class Handler {

		public static boolean onMessage(UpdateShieldPacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(new Runnable() {
				@Override
				public void run() {
					Entity entity = Minecraft.getInstance().level.getEntity(message.entityID);
					if (entity instanceof LivingEntity) {
						entity.getCapability(CapabilityList.SHIELDS).ifPresent(cap -> {
							cap.setShields(message.temporaryShields, true);
							cap.setShields(message.permanentShields, false);
						});
					}
				}
			});

			return true;
		}
	}
}
