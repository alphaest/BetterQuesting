package betterquesting.client.gui2.editors;

import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.client.gui.misc.IVolatileScreen;
import betterquesting.api.enums.EnumPacketAction;
import betterquesting.api.enums.EnumSaveType;
import betterquesting.api.network.QuestingPacket;
import betterquesting.api.questing.IQuest;
import betterquesting.api2.client.gui.GuiScreenCanvas;
import betterquesting.api2.client.gui.controls.IPanelButton;
import betterquesting.api2.client.gui.controls.PanelButton;
import betterquesting.api2.client.gui.controls.PanelButtonStorage;
import betterquesting.api2.client.gui.controls.PanelTextField;
import betterquesting.api2.client.gui.controls.filters.FieldFilterString;
import betterquesting.api2.client.gui.events.IPEventListener;
import betterquesting.api2.client.gui.events.PEventBroadcaster;
import betterquesting.api2.client.gui.events.PanelEvent;
import betterquesting.api2.client.gui.events.types.PEventButton;
import betterquesting.api2.client.gui.misc.*;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.CanvasTextured;
import betterquesting.api2.client.gui.panels.bars.PanelVScrollBar;
import betterquesting.api2.client.gui.panels.content.PanelLine;
import betterquesting.api2.client.gui.panels.content.PanelTextBox;
import betterquesting.api2.client.gui.panels.lists.CanvasQuestDatabase;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;
import betterquesting.api2.client.gui.themes.presets.PresetColor;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import betterquesting.api2.client.gui.themes.presets.PresetLine;
import betterquesting.api2.client.gui.themes.presets.PresetTexture;
import betterquesting.api2.storage.DBEntry;
import betterquesting.api2.utils.QuestTranslation;
import betterquesting.client.gui2.GuiQuest;
import betterquesting.network.PacketSender;
import betterquesting.network.PacketTypeNative;
import betterquesting.questing.QuestDatabase;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;

public class GuiPrerequisiteEditor extends GuiScreenCanvas implements IPEventListener, IVolatileScreen, INeedsRefresh
{
    private IQuest quest;
    private final int questID;
    
    private CanvasQuestDatabase canvasDB;
    private CanvasScrolling canvasPreReq;
    
    public GuiPrerequisiteEditor(GuiScreen parent, IQuest quest)
    {
        super(parent);
        this.quest = quest;
        this.questID = QuestDatabase.INSTANCE.getID(quest);
    }
    
    @Override
    public void refreshGui()
    {
        quest = QuestDatabase.INSTANCE.getValue(questID);
        
        if(quest == null)
        {
            mc.displayGuiScreen(parent);
            return;
        }
        
        canvasDB.refreshSearch();
        refreshReqCanvas();
    }
    
    @Override
    public void initPanel()
    {
        super.initPanel();
		
		PEventBroadcaster.INSTANCE.register(this, PEventButton.class);
        Keyboard.enableRepeatEvents(true);
        
        // Background panel
        CanvasTextured cvBackground = new CanvasTextured(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 0, 0, 0), 0), PresetTexture.PANEL_MAIN.getTexture());
        this.addPanel(cvBackground);
        
        PanelTextBox panTxt = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 0, -32), 0), QuestTranslation.translate("betterquesting.title.pre_requisites")).setAlignment(1);
        panTxt.setColor(PresetColor.TEXT_HEADER.getColor());
        cvBackground.addPanel(panTxt);
        
        cvBackground.addPanel(new PanelButton(new GuiTransform(GuiAlign.BOTTOM_CENTER, -100, -16, 200, 16, 0), 0, QuestTranslation.translate("gui.back")));
        
        // === RIGHT SIDE ===
        
        CanvasEmpty cvRight = new CanvasEmpty(new GuiTransform(GuiAlign.HALF_RIGHT, new GuiPadding(8, 32, 16, 24), 0));
        cvBackground.addPanel(cvRight);
        
        PanelTextBox txtDb = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 0, 0, -16), 0), QuestTranslation.translate("betterquesting.gui.database")).setAlignment(1);
        cvRight.addPanel(txtDb);
        
        PanelTextField<String> searchBox = new PanelTextField<>(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 16, 8, -32), 0), "", FieldFilterString.INSTANCE);
        searchBox.setWatermark("Search...");
        cvRight.addPanel(searchBox);
        
        canvasDB = new CanvasQuestDatabase(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 32, 8, 24), 0))
        {
            @Override
            protected boolean addResult(DBEntry<IQuest> entry, int index, int width)
            {
                PanelButtonStorage<DBEntry<IQuest>> btnAdd = new PanelButtonStorage<>(new GuiRectangle(0, index * 16, 16, 16, 0), 2, "", entry);
                btnAdd.setIcon(PresetIcon.ICON_POSITIVE.getTexture());
                btnAdd.setActive(!containsQuest(entry));
                this.addPanel(btnAdd);
                
                PanelButtonStorage<DBEntry<IQuest>> btnEdit = new PanelButtonStorage<>(new GuiRectangle(16, index * 16, width - 32, 16, 0), 1, QuestTranslation.translate(entry.getValue().getUnlocalisedName()), entry);
                this.addPanel(btnEdit);
                
                PanelButtonStorage<DBEntry<IQuest>> btnDel = new PanelButtonStorage<>(new GuiRectangle(width - 16, index * 16, 16, 16, 0), 4, "", entry);
                btnDel.setIcon(PresetIcon.ICON_TRASH.getTexture());
                this.addPanel(btnDel);
                
                return true;
            }
        };
        cvRight.addPanel(canvasDB);
        
        searchBox.setCallback(canvasDB::setSearchFilter);
    
        PanelVScrollBar scDb = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-8, 32, 0, 24), 0));
        cvRight.addPanel(scDb);
        canvasDB.setScrollDriverY(scDb);
        
        PanelButton btnNew = new PanelButton(new GuiTransform(GuiAlign.BOTTOM_EDGE, new GuiPadding(0, -16, 0, 0), 0), 5, QuestTranslation.translate("betterquesting.btn.new"));
        cvRight.addPanel(btnNew);
        
        // === LEFT SIDE ===
		
        CanvasEmpty cvLeft = new CanvasEmpty(new GuiTransform(GuiAlign.HALF_LEFT, new GuiPadding(16, 32, 8, 24), 0));
        cvBackground.addPanel(cvLeft);
        
        PanelTextBox txtQuest = new PanelTextBox(new GuiTransform(GuiAlign.TOP_EDGE, new GuiPadding(0, 0, 0, -16), 0), QuestTranslation.translate(quest.getUnlocalisedName())).setAlignment(1);
        cvLeft.addPanel(txtQuest);
        
        canvasPreReq = new CanvasScrolling(new GuiTransform(GuiAlign.FULL_BOX, new GuiPadding(0, 16, 8, 0), 0));
        cvLeft.addPanel(canvasPreReq);
        
        PanelVScrollBar scReq = new PanelVScrollBar(new GuiTransform(GuiAlign.RIGHT_EDGE, new GuiPadding(-8, 16, 0, 0), 0));
        cvLeft.addPanel(scReq);
        canvasPreReq.setScrollDriverY(scReq);
        
        // === DIVIDERS ===
        
		IGuiRect ls0 = new GuiTransform(GuiAlign.TOP_CENTER, 0, 32, 0, 0, 0);
		ls0.setParent(cvBackground.getTransform());
		IGuiRect le0 = new GuiTransform(GuiAlign.BOTTOM_CENTER, 0, -24, 0, 0, 0);
		le0.setParent(cvBackground.getTransform());
		PanelLine paLine0 = new PanelLine(ls0, le0, PresetLine.GUI_DIVIDER.getLine(), 1, PresetColor.GUI_DIVIDER.getColor(), 1);
		cvBackground.addPanel(paLine0);
		
		refreshReqCanvas();
    }
    
    private void refreshReqCanvas()
    {
        canvasPreReq.resetCanvas();
        int width = canvasPreReq.getTransform().getWidth();
        
        IQuest[] arrReq = quest.getPrerequisites().toArray(new IQuest[0]);
        for(int i = 0; i < arrReq.length; i++)
        {
            int reqID = QuestDatabase.INSTANCE.getID(arrReq[i]);
            PanelButtonStorage<DBEntry<IQuest>> btnEdit = new PanelButtonStorage<>(new GuiRectangle(0, i * 16, width - 16, 16, 0), 1, QuestTranslation.translate(arrReq[i].getUnlocalisedName()), new DBEntry<>(reqID, arrReq[i]));
            canvasPreReq.addPanel(btnEdit);
            
            PanelButtonStorage<DBEntry<IQuest>> btnRem = new PanelButtonStorage<>(new GuiRectangle(width - 16, i * 16, 16, 16, 0), 3, "", new DBEntry<>(reqID, arrReq[i]));
            btnRem.setIcon(PresetIcon.ICON_NEGATIVE.getTexture());
            canvasPreReq.addPanel(btnRem);
        }
    }
    
    private boolean containsQuest(DBEntry<IQuest> entry)
    {
        IQuest[] arrReq = quest.getPrerequisites().toArray(new IQuest[0]);
        for(IQuest anArrReq : arrReq)
        {
            if(entry.getValue() == anArrReq) return true;
        }
        
        return false;
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
        } else if(btn.getButtonID() == 1 && btn instanceof PanelButtonStorage) // Edit Quest
        {
            DBEntry<IQuest> entry = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            mc.displayGuiScreen(new GuiQuest(this, entry.getID()));
        } else if(btn.getButtonID() == 2 && btn instanceof PanelButtonStorage) // Add
        {
            DBEntry<IQuest> entry = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            quest.getPrerequisites().add(entry.getValue());
            SendChanges();
        } else if(btn.getButtonID() == 3 && btn instanceof PanelButtonStorage) // Remove
        {
            DBEntry<IQuest> entry = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            quest.getPrerequisites().remove(entry.getValue());
            SendChanges();
        } else if(btn.getButtonID() == 4 && btn instanceof PanelButtonStorage) // Delete
        {
            DBEntry<IQuest> entry = ((PanelButtonStorage<DBEntry<IQuest>>)btn).getStoredValue();
            NBTTagCompound tags = new NBTTagCompound();
            tags.setInteger("action", EnumPacketAction.REMOVE.ordinal());
            tags.setInteger("questID", entry.getID());
            PacketSender.INSTANCE.sendToServer(new QuestingPacket(PacketTypeNative.QUEST_EDIT.GetLocation(), tags));
        } else if(btn.getButtonID() == 5) // New
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("action", EnumPacketAction.ADD.ordinal());
            PacketSender.INSTANCE.sendToServer(new QuestingPacket(PacketTypeNative.QUEST_EDIT.GetLocation(), tag));
        }
    }
	
	private void SendChanges()
	{
		NBTTagCompound tags = new NBTTagCompound();
		NBTTagCompound base = new NBTTagCompound();
		base.setTag("config", quest.writeToNBT(new NBTTagCompound(), EnumSaveType.CONFIG));
		base.setTag("progress", quest.writeToNBT(new NBTTagCompound(), EnumSaveType.PROGRESS));
		tags.setTag("data", base);
		tags.setInteger("questID", questID);
		tags.setInteger("action", EnumPacketAction.EDIT.ordinal());
		PacketSender.INSTANCE.sendToServer(new QuestingPacket(PacketTypeNative.QUEST_EDIT.GetLocation(), tags));
	}
}
