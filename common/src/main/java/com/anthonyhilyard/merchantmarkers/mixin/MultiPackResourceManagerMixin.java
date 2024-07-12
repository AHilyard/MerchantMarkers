package com.anthonyhilyard.merchantmarkers.mixin;

import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

@Mixin(MultiPackResourceManager.class)
public class MultiPackResourceManagerMixin
{
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"))
	private List<PackResources> mutablePacks(Collection<? extends PackResources> packs)
	{
		return Lists.newArrayList(packs);
	}
}