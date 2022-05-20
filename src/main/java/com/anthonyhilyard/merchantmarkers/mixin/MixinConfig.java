package com.anthonyhilyard.merchantmarkers.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;

public class MixinConfig implements IMixinConfigPlugin
{
	private LoadingModList loadingModList = null;

	@Override
	public void onLoad(String mixinPackage) { }

	@Override
	public String getRefMapperConfig() { return null; }

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
	{
		if (loadingModList == null)
		{
			loadingModList = FMLLoader.getLoadingModList();
		}

		// Only apply mixins with "ftbchunks" in the name if the mod "ftbchunks" is present.
		if (mixinClassName.toLowerCase().contains("ftbchunks"))
		{
			return loadingModList.getMods().stream().anyMatch(modInfo -> modInfo.getModId().contentEquals("ftbchunks"));
		}

		// Same for "journeymap".
		if (mixinClassName.toLowerCase().contains("journeymap"))
		{
			return loadingModList.getMods().stream().anyMatch(modInfo -> modInfo.getModId().contentEquals("journeymap"));
		}

		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

	@Override
	public List<String> getMixins() { return null; }

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}