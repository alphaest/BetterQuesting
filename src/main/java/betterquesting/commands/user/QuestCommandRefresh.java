package betterquesting.commands.user;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.UUID;

import betterquesting.api.api.ApiReference;
import betterquesting.api.api.QuestingAPI;
import betterquesting.commands.QuestCommandBase;
import betterquesting.network.PacketSender;
import betterquesting.questing.QuestDatabase;
import betterquesting.questing.QuestLineDatabase;
import betterquesting.storage.LifeDatabase;
import betterquesting.storage.NameCache;
import betterquesting.storage.QuestSettings;

public class QuestCommandRefresh extends QuestCommandBase
{
	@Override
	public String getCommand()
	{
		return "refresh";
	}
	
	@Override
	public void runCommand(MinecraftServer server, CommandBase command, ICommandSender sender, String[] args) throws CommandException
	{
		if(sender instanceof EntityPlayerMP)
		{
			EntityPlayerMP player = (EntityPlayerMP)sender;
			UUID uuid = QuestingAPI.getAPI(ApiReference.NAME_CACHE).registerAndGetUUID(player);
			
			PacketSender.INSTANCE.sendToPlayer(QuestDatabase.INSTANCE.getSyncPrivatePacket(uuid), player);
			PacketSender.INSTANCE.sendToPlayer(QuestLineDatabase.INSTANCE.getSyncPacket(), player);
			PacketSender.INSTANCE.sendToPlayer(LifeDatabase.INSTANCE.getSyncPrivatePacket(uuid), player);
			PacketSender.INSTANCE.sendToPlayer(NameCache.INSTANCE.getSyncPacket(), player);
			PacketSender.INSTANCE.sendToPlayer(QuestSettings.INSTANCE.getSyncPacket(), player);
			sender.sendMessage(new TextComponentTranslation("betterquesting.cmd.refresh"));
		}
	}
}
