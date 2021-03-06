package betterquesting.client.gui2.editors;

import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.client.gui.misc.IVolatileScreen;
import betterquesting.api.enums.EnumPacketAction;
import betterquesting.api.enums.EnumSaveType;
import betterquesting.api.misc.IFactory;
import betterquesting.api.network.QuestingPacket;
import betterquesting.api.questing.IQuest;
import betterquesting.api.questing.tasks.ITask;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.*;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.editors.nbt.GuiNbtEditor;
import betterquesting.network.PacketSender;
import betterquesting.network.PacketTypeNative;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.tasks.TaskRegistry;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.util.vector.Vector4f;

import java.util.List;

public class GuiTaskEditor extends GuiScreenCanvas implements IPEventListener, IVolatileScreen, INeedsRefresh
{
	private CanvasScrolling qrList;
	
    private IQuest quest;
    private final int qID;
    
    public GuiTaskEditor(GuiScreen parent, IQuest quest)
    {
        super(parent);
        
        this.quest = quest;
        this.qID = QuestDatabase.INSTANCE.getID(quest);
    }
	
	@Override
	public void refreshGui()
	{
	    quest = QuestDatabase.INSTANCE.getValue(qID);
	    
	    if(quest == null)
        {
            mc.displayGuiScreen(this.parent);
            return;
        }
        
        refreshRewards();
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
	    
	    if(qID < 0)
        {
            mc.displayGuiScreen(this.parent);
            return;
        }
		
		PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
        
        // Background panel
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        
        PanelTextBox panTxt = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), QuestTranslation.translate("betterquesting.title.edit_tasks")).setAlignment(1);
        panTxt.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(panTxt);
        
        cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 200, 16, 0), 0, QuestTranslation.translate("gui.back")));
        
        CanvasScrolling rewReg = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(8, 32, 24, 32), 0));
        cvBackground.addPanel(rewReg);
        PanelVScrollBar scReg = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-24, 32, 16, 32), 0));
        cvBackground.addPanel(scReg);
        rewReg.setScrollDriverY(scReg);
        
        int w = rewReg.getTransform().getWidth();
        List<IFactory<? extends ITask>> tmp = TaskRegistry.INSTANCE.getAll();
        
        for(int i = 0; i < tmp.size(); i++)
        {
            IFactory<? extends ITask> rewFact = tmp.get(i);
            rewReg.addPanel(new PanelButtonStorage<IFactory<? extends ITask>>(new GuiRectangle(0, i * 16, w, 16, 0), 1, rewFact.getRegistryName().toString(), rewFact));
        }
        
        qrList = new CanvasScrolling(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(16, 32, 16, 32), 0));
        cvBackground.addPanel(qrList);
        
        PanelVScrollBar scRew = new PanelVScrollBar(new GuiTransform(new Vector4f(0.5F, 0F, 0.5F, 1F), new GuiPadding(-16, 32, 8, 32), 0));
        cvBackground.addPanel(scRew);
        qrList.setScrollDriverY(scRew);
		
        // === DIVIDERS ===
        
		IGuiRect ls0 = new GuiTransform(GuiAlign.TOP_CENTER, 0, 32, 0, 0, 0);
		ls0.setParent(cvBackground.getTransform());
		IGuiRect le0 = new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, -32, 0, 0, 0);
		le0.setParent(cvBackground.getTransform());
		PanelLine paLine0 = new PanelLine(ls0, le0, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
		cvBackground.addPanel(paLine0);
        
        refreshRewards();
    }
	
	@Override
	public void onPanelEvent(PanelEvent event)
	{
		if(event instanceof PEventButton)
		{
			onButtonPress((PEventButton)event);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void onButtonPress(PEventButton event)
	{
        IPanelButton btn = event.getButton();
        
        if(btn.getButtonID() == 0) // Exit
        {
            mc.displayGuiScreen(this.parent);
        } else if(btn.getButtonID() ==  1 && btn instanceof PanelButtonStorage) // Add
        {
            IFactory<? extends ITask> fact = ((PanelButtonStorage<IFactory<? extends ITask>>)btn).getStoredValue();
            quest.getTasks().add(quest.getTasks().nextID(), fact.createNew());
            
            SendChanges();
        } else if(btn.getButtonID() == 2 && btn instanceof PanelButtonStorage) // Remove
        {
            ITask reward = ((PanelButtonStorage<ITask>)btn).getStoredValue();
            
            if(quest.getTasks().removeValue(reward))
            {
                SendChanges();
            }
        } else if(btn.getButtonID() == 3 && btn instanceof PanelButtonStorage) // Edit
        {
            ITask reward = ((PanelButtonStorage<ITask>)btn).getStoredValue();
            GuiScreen editor = reward.getTaskEditor(this, quest);
            
            if(editor != null)
            {
                mc.displayGuiScreen(editor);
            } else
            {
                mc.displayGuiScreen(new GuiNbtEditor(this, reward.writeToNBT(new NBTTagCompound(), EnumSaveType.CONFIG), value -> {
                    reward.readFromNBT(value, EnumSaveType.CONFIG);
                    
                    NBTTagCompound base = new NBTTagCompound();
                    base.setTag("config", quest.writeToNBT(new NBTTagCompound(), EnumSaveType.CONFIG));
                    base.setTag("progress", quest.writeToNBT(new NBTTagCompound(), EnumSaveType.PROGRESS));
                    NBTTagCompound tags = new NBTTagCompound();
                    tags.setInteger("action", EnumPacketAction.EDIT.ordinal()); // Action: Update data
                    tags.setInteger("questID", qID);
                    tags.setTag("data", base);
                    PacketSender.INSTANCE.sendToServer(new QuestingPacket(PacketTypeNative.QUEST_EDIT.GetLocation(), tags));
                }));
            }
        }
    }
    
    private void refreshRewards()
    {
        DBEntry<ITask>[] dbRew = quest.getTasks().getEntries();
        
        qrList.resetCanvas();
        int w = qrList.getTransform().getWidth();
        
        for(int i = 0; i < dbRew.length; i++)
        {
            ITask reward = dbRew[i].getValue();
            qrList.addPanel(new PanelButtonStorage<>(new GuiRectangle(0, i * 16, w - 16, 16, 0), 3, QuestTranslation.translate(reward.getUnlocalisedName()), reward));
            qrList.addPanel(new PanelButtonStorage<>(new GuiRectangle(w - 16, i * 16, 16, 16, 0), 2, "" + TextFormatting.RED + TextFormatting.BOLD + "x", reward));
        }
    }
	
	private void SendChanges()
	{
		NBTTagCompound base = new NBTTagCompound();
		base.setTag("config", quest.writeToNBT(new NBTTagCompound(), EnumSaveType.CONFIG));
		base.setTag("progress", quest.writeToNBT(new NBTTagCompound(), EnumSaveType.PROGRESS));
		NBTTagCompound tags = new NBTTagCompound();
		tags.setInteger("action", EnumPacketAction.EDIT.ordinal()); // Action: Update data
		tags.setInteger("questID", qID);
		tags.setTag("data",base);
		PacketSender.INSTANCE.sendToServer(new QuestingPacket(PacketTypeNative.QUEST_EDIT.GetLocation(), tags));
	}
}
