package com.mcjty.rftools.commands;

import com.mcjty.rftools.items.dimlets.KnownDimletConfiguration;
import net.minecraft.command.ICommandSender;

public class CmdDumpRarity extends AbstractRfToolsCommand {
    @Override
    public String getHelp() {
        return "[<tries> [<bonus>]]";
    }

    @Override
    public String getCommand() {
        return "dumprarity";
    }

    @Override
    public void execute(ICommandSender sender, String[] args) {
        int tries = fetchInt(sender, args, 1, 1);
        int bonus = fetchInt(sender, args, 2, 0);

        KnownDimletConfiguration.dumpRarityDistribution(tries, bonus);
    }
}