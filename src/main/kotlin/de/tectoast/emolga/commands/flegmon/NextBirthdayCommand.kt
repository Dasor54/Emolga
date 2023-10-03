package de.tectoast.emolga.commands.flegmon

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.PepeCommand
import de.tectoast.emolga.database.exposed.BirthdayDB
import net.dv8tion.jda.api.entities.Member
import java.util.*
import java.util.function.Consumer

object NextBirthdayCommand : PepeCommand("nextbirthday", "Zeigt die naheliegende Geburtstage an") {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        aliases.add("nextbirthdays")
    }

    override suspend fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        val curr = Calendar.getInstance()
        curr[Calendar.HOUR_OF_DAY] = 0
        curr[Calendar.MINUTE] = 0
        curr[Calendar.SECOND] = 0
        val map: MutableMap<Long, Calendar> = HashMap()
        for (bData in BirthdayDB.all) {
            val c = Calendar.getInstance()
            c[Calendar.DAY_OF_MONTH] = bData.day
            c[Calendar.MONTH] = bData.month - 1
            c[Calendar.YEAR] = if (curr.timeInMillis - c.timeInMillis >= 0) c[Calendar.YEAR] + 1 else c[Calendar.YEAR]
            c[Calendar.HOUR_OF_DAY] = 0
            c[Calendar.MINUTE] = 0
            c[Calendar.SECOND] = 0
            val dif = c.timeInMillis - curr.timeInMillis
            if (dif <= 1209600000 /* && dif >= -432000000*/) {
                map[bData.userid] = c
            }
        }
        if (map.isEmpty()) {
            tco.sendMessage("Es gibt keine nahegelegenen Geburtstage!").queue()
            return
        }
        val str = StringBuilder("Die nächsten Geburtstage:\n\n")
        val names = HashMap<Long, String>()
        e.guild.retrieveMembersByIds(map.keys).get()
            .forEach(Consumer { mem: Member -> names[mem.idLong] = mem.effectiveName })
        map.keys.sortedBy { map[it]!!.timeInMillis }.forEach {
            val c = map[it]
            str.append("`").append(c!![Calendar.DAY_OF_MONTH].toString().padStart(2, '0')).append(".").append(

                (c[Calendar.MONTH] + 1).toString().padStart(2, '0')

            ).append(".").append("`: ").append(names[it]).append("\n")
        }
        tco.sendMessage(str.toString()).queue()
    }
}
