// Date: 4/25/2012 10:28:13 PM
// Template version 1.1
// Java generated by Techne
// Keep in mind that you still need to fill in some blanks
// - ZeuX

package twilightforest.client.model.entity.legacy;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import twilightforest.entity.passive.TinyBirdEntity;

public class TinyBirdLegacyModel extends AgeableListModel<TinyBirdEntity> {
	//fields
	ModelPart head;
	ModelPart body;
	ModelPart rightarm;
	ModelPart leftarm;
	ModelPart rightleg;
	ModelPart leftleg;
	ModelPart tail;

	public TinyBirdLegacyModel(ModelPart root) {
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.rightarm = root.getChild("right_arm");
		this.leftarm = root.getChild("left_arm");
		this.rightleg = root.getChild("right_leg");
		this.leftleg = root.getChild("left_leg");
		this.tail = root.getChild("tail");
	}

	public static LayerDefinition create() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition partRoot = mesh.getRoot();

		var head = partRoot.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(0, 0)
						.addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3),
				PartPose.offset(0F, 20.5F, -0.5F));

		head.addOrReplaceChild("beak", CubeListBuilder.create()
						.texOffs(12, 0)
						.addBox(-0.5F, -0.5F, -0.5F, 1, 1, 1),
				PartPose.offset(0F, 0.5F, -2F));

		partRoot.addOrReplaceChild("body", CubeListBuilder.create()
						.texOffs(0, 6)
						.addBox(-1.5F, 0F, -1F, 3, 3, 3),
				PartPose.offset(0F, 20F, 0F));

		partRoot.addOrReplaceChild("right_arm", CubeListBuilder.create()
						.texOffs(12, 2)
						.addBox(-1F, 0F, -1.5F, 1, 2, 3),
				PartPose.offset(-1.5F, 20.5F, 1F));

		partRoot.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
						.texOffs(12, 2)
						.addBox(0F, 0F, -1.5F, 1, 2, 3),
				PartPose.offset(1.5F, 20.5F, 1F));

		partRoot.addOrReplaceChild("right_leg", CubeListBuilder.create()
						.texOffs(0, 12)
						.addBox(0F, 0F, 0F, 1, 1, 1),
				PartPose.offset(-1.5F, 23F, 0F));

		partRoot.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
						.texOffs(0, 12)
						.addBox(0F, 0F, 0F, 1, 1, 1),
				PartPose.offset(0F, 23F, 0F));

		partRoot.addOrReplaceChild("tail", CubeListBuilder.create()
						.texOffs(0, 14)
						.addBox(-1.5F, -0.5F, 0F, 3, 1, 2),
				PartPose.offset(0F, 22F, 2F));

		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	protected Iterable<ModelPart> headParts() {
		return ImmutableList.of(this.head);
	}

	@Override
	protected Iterable<ModelPart> bodyParts() {
		return ImmutableList.of(
				head,
				body,
				rightleg,
				leftleg,
				rightarm,
				leftarm,
				tail
		);
	}

	@Override
	public void renderToBuffer(PoseStack stack, VertexConsumer builder, int light, int overlay, float red, float green, float blue, float scale) {
		if (young) {
			float f = 2.0F;
			stack.pushPose();
			stack.translate(0.0F, 5F * scale, 0.75F * scale);
			this.headParts().forEach((renderer) -> renderer.render(stack, builder, light, overlay, red, green, blue, scale));
			stack.popPose();
			stack.pushPose();
			stack.scale(1.0F / f, 1.0F / f, 1.0F / f);
			stack.translate(0.0F, 24F * scale, 0.0F);
			this.bodyParts().forEach((renderer) -> renderer.render(stack, builder, light, overlay, red, green, blue, scale));
			stack.popPose();
		} else {
			this.headParts().forEach((renderer) -> renderer.render(stack, builder, light, overlay, red, green, blue, scale));
			this.bodyParts().forEach((renderer) -> renderer.render(stack, builder, light, overlay, red, green, blue, scale));
		}
	}

	/**
	 * Sets the models various rotation angles.
	 */
	@Override
	public void setupAnim(TinyBirdEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		head.xRot = headPitch / (180F / (float) Math.PI);
		head.yRot = netHeadYaw / (180F / (float) Math.PI);

		rightleg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		leftleg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

		rightarm.zRot = ageInTicks;
		leftarm.zRot = -ageInTicks;

		if (entity.isBirdLanded()) {
			rightleg.y = 23;
			leftleg.y = 23;
		} else {
			rightleg.y = 22.5F;
			leftleg.y = 22.5F;
		}
	}
}
