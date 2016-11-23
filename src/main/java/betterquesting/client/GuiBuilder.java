package betterquesting.client;

import java.io.File;
import java.io.FileFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraftforge.fluids.FluidStack;
import betterquesting.api.client.gui.misc.IGuiHelper;
import betterquesting.api.jdoc.IJsonDoc;
import betterquesting.api.other.ICallback;
import betterquesting.api.other.IMultiCallback;
import betterquesting.api.utils.BigItemStack;
import betterquesting.client.gui.editors.GuiTextEditor;
import betterquesting.client.gui.editors.json.GuiJsonArray;
import betterquesting.client.gui.editors.json.GuiJsonEntitySelection;
import betterquesting.client.gui.editors.json.GuiJsonFluidSelection;
import betterquesting.client.gui.editors.json.GuiJsonItemSelection;
import betterquesting.client.gui.editors.json.GuiJsonObject;
import betterquesting.client.gui.misc.GuiFileExplorer;
import com.google.gson.JsonElement;

public final class GuiBuilder implements IGuiHelper
{
	public static final GuiBuilder INSTANCE = new GuiBuilder();
	
	private final Minecraft mc;
	
	private GuiBuilder()
	{
		this.mc = Minecraft.getMinecraft();
	}
	
	@Override
	public <T extends JsonElement> void openJsonEditor(GuiScreen parent, ICallback<T> callback, T json, IJsonDoc jdoc)
	{
		if(json.isJsonArray())
		{
			mc.displayGuiScreen(new GuiJsonArray(parent, json.getAsJsonArray(), jdoc));
		} else if(json.isJsonObject())
		{
			mc.displayGuiScreen(new GuiJsonObject(parent, json.getAsJsonObject(), jdoc));
		}
		
		return;
	}
	
	@Override
	public void openItemEditor(GuiScreen parent, ICallback<BigItemStack> callback, BigItemStack stack)
	{
		mc.displayGuiScreen(new GuiJsonItemSelection(parent, callback, stack));
	}
	
	@Override
	public void openFluidEditor(GuiScreen parent, ICallback<FluidStack> callback, FluidStack stack)
	{
		mc.displayGuiScreen(new GuiJsonFluidSelection(parent, callback, stack));
	}
	
	@Override
	public void openEntityEditor(GuiScreen parent, ICallback<Entity> callback, Entity entity)
	{
		mc.displayGuiScreen(new GuiJsonEntitySelection(parent, callback, entity));
	}
	
	@Override
	public void openTextEditor(GuiScreen parent, ICallback<String> callback, String text)
	{
		GuiTextEditor gui = new GuiTextEditor(parent, text);
		
		if(callback != null)
		{
			gui.setHost(callback);
		}
		
		mc.displayGuiScreen(gui);
	}
	
	@Override
	public void openFileExplorer(GuiScreen parent, IMultiCallback<File> callback, File rootDir, FileFilter filter, boolean multiSelect)
	{
		mc.displayGuiScreen(new GuiFileExplorer(parent, callback, rootDir, filter).setMultiSelect(multiSelect));
	}
	
}
