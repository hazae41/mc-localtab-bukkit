package hazae41.localtab

fun getLang(locale: String): Lang {
  val first = locale.split("_")[0]

  if (first == "en")
    return English
  if (first == "fr")
    return French
  if (first == "es")
    return Spanish
  if (first == "ca")
    return Spanish
  if (first == "it")
    return Italian
  if (first == "de")
    return German
  if (first == "nl")
    return Dutch
  if (first == "cs")
    return Czech
  if (first == "ru")
    return Russian
  if (first == "pl")
    return Polish
  if (first == "zh")
    return Chinese
  if (first == "ja")
    return Japanese
  if (first == "ko")
    return Korean

  return English
}

interface Lang {
  fun header(): String
}

object English : Lang {
  override fun header() = "Close players"
}

object French : Lang {
  override fun header() = "Joueurs proches"
}

object Spanish : Lang {
  override fun header() = "Jugadores cercanos"
}

object Italian : Lang {
  override fun header() = "Giocatori vicini"
}

object German : Lang {
  override fun header() = "Enge Spieler"
}

object Dutch : Lang {
  override fun header() = "Nauwe spelers"
}

object Czech : Lang {
  override fun header() = "Blízcí hráči"
}

object Russian : Lang {
  override fun header() = "Близкие игроки"
}

object Polish : Lang {
  override fun header() = "Bliscy gracze"
}

object Chinese : Lang {
  override fun header() = "亲密玩家"
}

object Japanese : Lang {
  override fun header() = "近いプレーヤー"
}

object Korean : Lang {
  override fun header() = "가까운 플레이어"
}