package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.Translation
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.draft.Tierlist


object TierlistSearchCommand :
        Command("tierlistsearch", "Zeigt dir alle Pokemon in einem Tier mit einem bestimmten Typ", CommandCategory.Draft) {

    init {
        argumentTemplate = ArgumentManagerTemplate.create {
            add("tier", "Tier", "Das Tier, in dem du suchen möchtest", ArgumentManagerTemplate.Text.any())
            addEngl("type", "Typ", "Der Typ, den du suchen möchtest", Translation.Type.TYPE)
            example = "!tierlistsearch A Water"
        }
        slash(true, Constants.G.ASL)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val gid = e.guild.idLong
        val tierlist = Tierlist[gid] ?: run {
            e.reply("Es wurde keine Tierliste für diesen Server hinterlegt!")
            return
        }
        val tier = args.getText("tier")
        val mons = tierlist.getByTier(tier) ?: run {
            e.reply("Das Tier existiert auf diesem Server nicht!")
            return
        }
        val searchType = args.getTranslation("type")
        val searchTypeEnglish = searchType.translation
        val filteredList = mons.filter { searchTypeEnglish in getDataObject(it, gid).types }
        e.reply(
            "All diese Mons aus dem ${tier}-Tier besitzen den Typen ${searchType.otherLang}:\n${
                filteredList.joinToString(
                    "\n"
                )
            }", ephemeral = true
        )
    }

}
