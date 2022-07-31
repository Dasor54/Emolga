package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.Emolga

class DraftsetupCommand : Command("draftsetup", "Startet das Draften der Liga in diesem Channel", CommandCategory.Flo) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("name", "Name", "Der Name der Liga/des Drafts", ArgumentManagerTemplate.Text.any())
            .add(
                "tc",
                "Channel",
                "Der Channel, wo die Teamübersicht sein soll",
                ArgumentManagerTemplate.DiscordType.CHANNEL,
                true
            )
            .add(
                "switch",
                "Switch-Draft",
                "Ob es ein Switch-Draft sein soll",
                ArgumentManagerTemplate.ArgumentBoolean,
                optional = true
            )
            .add(
                "fromfile",
                "Datei",
                "Ob alte Daten verwendet werden sollen",
                ArgumentManagerTemplate.ArgumentBoolean,
                optional = true
            )
            .setExample("!draftsetup Emolga-Conference #emolga-teamübersicht")
            .build()
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114L))
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments
        Emolga.get.league(args.getText("name")).startDraft(
            e.textChannel,
            isSwitchDraft = args.getOrDefault("switch", false),
            fromFile = args.getOrDefault("fromfile", false)
        )
    }
}