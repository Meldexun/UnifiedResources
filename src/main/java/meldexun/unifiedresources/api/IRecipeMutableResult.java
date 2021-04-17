package meldexun.unifiedresources.api;

import net.minecraft.item.ItemStack;

public interface IRecipeMutableResult {

	int getResultItemCount();

	ItemStack getResultItem(int index);

	void setResultItem(ItemStack stack, int index);

}
