package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist

class AddToTierlistCommand :
    Command("addtotierlist", "Fügt ein Mon ins D-Tier ein", CommandCategory.Draft, Constants.G.ASL) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("mon", "Mon", "Das Mon", ArgumentManagerTemplate.draftPokemon(), false, "Das ist kein Pokemon!")
            .add("tier", "Tier", "Das Tier, sonst das unterste", ArgumentManagerTemplate.Text.any(), true)
            .setExample("!addtotierlist Chimstix")
            .build()
        setCustomPermissions(PermissionPreset.fromRole(702233714360582154L))
    }

    override suspend fun process(e: GuildCommandEvent) {
        val id = e.guild.id
        val o: Tierlist = load("./Tierlists/$id.json")
        val mon = e.arguments.getText("mon")
        if (e.arguments.has("tier")) o.additionalMons
            .getOrPut(e.arguments.getText("tier")) { mutableListOf() }.add(mon)
        else o.trashmons.add(mon)
        save(o, "./Tierlists/$id.json")
        Tierlist.setup()
        e.reply("`$mon` ist nun pickable!")
    }
}