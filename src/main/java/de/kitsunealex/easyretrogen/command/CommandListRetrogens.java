package de.kitsunealex.easyretrogen.command;

import com.google.common.collect.Lists;
import de.kitsunealex.easyretrogen.retrogen.TargetWorldWrapper;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.List;
import java.util.Set;

public class CommandListRetrogens extends CommandBase {

    @Override
    public String getName() {
        return "listretrogens";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "List retrogens";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Set<IWorldGenerator> worldGens = ObfuscationReflectionHelper.getPrivateValue(GameRegistry.class, null, "worldGenerators");
        List<String> targets = Lists.newArrayList();

        for(IWorldGenerator generator : worldGens) {
            if(!(generator instanceof TargetWorldWrapper)) {
                targets.add(generator.getClass().getName());
            }
        }

        if(targets.isEmpty()) {
            sender.sendMessage(new TextComponentString("There are no retrogen target classes"));
        }
        else {
            sender.sendMessage(new TextComponentString(CommandBase.joinNiceStringFromCollection(targets)));
        }
    }

}
