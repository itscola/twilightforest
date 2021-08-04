// Date: 4/27/2012 9:49:06 PM
// Template version 1.1
// Java generated by Techne
// Keep in mind that you still need to fill in some blanks
// - ZeuX

package twilightforest.client.model.entity.legacy;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import twilightforest.entity.passive.SquirrelEntity;

public class SquirrelLegacyModel extends QuadrupedModel<SquirrelEntity> {
	//fields
	ModelPart tail;
	ModelPart fluff1;
	ModelPart fluff2;
	ModelPart fluff3;

	public SquirrelLegacyModel(ModelPart root) {
		super(root, false, 4.0F, 4.0F, 2.0F, 2.0F, 24);
		this.tail = body.getChild("tail");
		this.fluff1 = tail.getChild("fluff_1");
		this.fluff2 = fluff1.getChild("fluff_2");
		this.fluff3 = fluff2.getChild("fluff_3");
	}

	public static LayerDefinition create() {
		MeshDefinition mesh = QuadrupedModel.createBodyMesh(0, CubeDeformation.NONE);
		PartDefinition partRoot = mesh.getRoot();

		partRoot.addOrReplaceChild("head", CubeListBuilder.create()
						.texOffs(0, 0)
						.addBox(-2F, -5F, -3F, 4, 4, 4)
						.texOffs(16, 0)
						.addBox(-2F, -6F, -0.5F, 1, 1, 1)
						.texOffs(16, 0)
						.addBox(1F, -6F, -0.5F, 1, 1, 1),
				PartPose.offset(0F, 22F, -2F));

		var body = partRoot.addOrReplaceChild("body", CubeListBuilder.create().mirror()
						.texOffs(0, 0)
						.addBox(-2F, -1F, -2F, 4, 3, 5),
				PartPose.offset(0F, 21F, 0F));

		partRoot.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().mirror()
						.texOffs(0, 16)
						.addBox(0F, 0F, 0F, 1, 1, 1),
				PartPose.offset(-2F, 23F, 2F));

		partRoot.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().mirror()
						.texOffs(0, 16)
						.addBox(0F, 0F, 0F, 1, 1, 1),
				PartPose.offset(1F, 23F, 2F));

		partRoot.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
						.texOffs(0, 16)
						.addBox(0F, 0F, 0F, 1, 1, 1),
				PartPose.offset(-2F, 23F, -2F));

		partRoot.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
						.texOffs(0, 16)
						.addBox(0F, 0F, 0F, 1, 1, 1),
				PartPose.offset(1F, 23F, -2F));

		var tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
						.texOffs(0, 18)
						.addBox(-0.5F, -1.5F, 0.5F, 1, 1, 1),
				PartPose.offset(0F, 21F, 2F));

		var fluff1 = tail.addOrReplaceChild("fluff_1", CubeListBuilder.create()
						.texOffs(0, 20)
						.addBox(-1.5F, -4F, 1F, 3, 3, 3),
				PartPose.ZERO);

		var fluff2 = fluff1.addOrReplaceChild("fluff_2", CubeListBuilder.create()
						.texOffs(0, 20)
						.addBox(0F, -3F, -1.5F, 3, 3, 3),
				PartPose.offset(-1.5F, -4F, 2.5F));

		fluff2.addOrReplaceChild("fluff_3", CubeListBuilder.create()
						.texOffs(0, 26)
						.addBox(1.5F, -3F, -1.5F, 3, 3, 3),
				PartPose.offset(-1.5F, -3F, 0F));

		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(SquirrelEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		this.head.xRot = headPitch / (180F / (float) Math.PI);
		this.head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.rightHindLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
		this.leftHindLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.rightFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
		this.leftFrontLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;

		if (limbSwingAmount > 0.2) {
			float wiggle = Math.min(limbSwingAmount, 0.6F);
			this.tail.xRot = (Mth.cos(ageInTicks * 0.6662F) - (float) Math.PI / 3) * wiggle;
			this.fluff2.xRot = Mth.cos(ageInTicks * 0.7774F) * 1.2F * wiggle;
			this.fluff3.xRot = Mth.cos(ageInTicks * 0.8886F + (float) Math.PI / 2) * 1.4F * wiggle;
		} else {
			this.tail.xRot = 0.2F + Mth.cos(ageInTicks * 0.3335F) * 0.15F;
			this.fluff2.xRot = 0.1F + Mth.cos(ageInTicks * 0.4445F) * 0.20F;
			this.fluff3.xRot = 0.1F + Mth.cos(ageInTicks * 0.5555F) * 0.25F;
		}
	}
}
