package de.tectoast.emolga.commands.soullink

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only

object StatusCommand : Command("status", "Setzt den Status eines Encounters", CommandCategory.Soullink) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("location", "Location", "Die Location", ArgumentManagerTemplate.Text.withAutocomplete { s, _ ->
                db.soullink.only().order.filter { it.startsWith(s, ignoreCase = true) }
            })
            .add(
                "status",
                "Status",
                "Der Status",
                ArgumentManagerTemplate.Text.of(SubCommand.of("Team"), SubCommand.of("Box"), SubCommand.of("RIP"))
            )
            .setExample("/status Route 1 RIP")
            .build()
        slash(true, 695943416789598208L)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val soullink = db.soullink.only()
        val args = e.arguments
        val location = eachWordUpperCase(args.getText("location"))
        if (location !in soullink.order) {
            e.reply("Diese Location ist derzeit nicht im System!")
            return
        }
        soullink.mons[location]!!["status"] = args.getText("status")
        e.reply("\uD83D\uDC4D")
        soullink.save()
        updateSoullink()
    }
}
