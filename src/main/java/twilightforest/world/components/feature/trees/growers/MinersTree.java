package twilightforest.world.components.feature.trees.growers;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import twilightforest.world.registration.features.TFConfiguredFeatures;

public class MinersTree extends TFTree {

	@Override
	public Holder<? extends ConfiguredFeature<?, ?>> createTreeFeature() {
		return Holder.direct(TFConfiguredFeatures.MINING_TREE.get());
	}
}
