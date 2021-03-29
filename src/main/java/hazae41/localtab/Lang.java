package hazae41.localtab;

class Langs {
  static Lang getLang(String locale) {
    String first = locale.split("_")[0];

    if (first.equals("en"))
      return new English();
    if (first.equals("fr"))
      return new French();
    if (first.equals("ca"))
      return new Spanish();
    if (first.equals("es"))
      return new Spanish();
    if (first.equals("pt"))
      return new Portuguese();
    if (first.equals("it"))
      return new Italian();
    if (first.equals("de"))
      return new German();
    if (first.equals("nl"))
      return new Dutch();
    if (first.equals("cs"))
      return new Czech();
    if (first.equals("ru"))
      return new Russian();
    if (first.equals("pl"))
      return new Polish();
    if (first.equals("ja"))
      return new Japanese();
    if (first.equals("ko"))
      return new Korean();
    if (first.equals("zh"))
      return new Chinese();

    return new English();
  }
}

public abstract class Lang {
  abstract String getHeader();
}

class English extends Lang {
  @Override
  String getHeader() {
    return "Close players";
  }
}

class French extends Lang {
  @Override
  String getHeader() {
    return "Joueurs proches";
  }
}

class Spanish extends Lang {
  @Override
  String getHeader() {
    return "Jugadores cercanos";
  }
}

class Portuguese extends Lang {
  @Override
  String getHeader() {
    return "Jogadores próximos";
  }
}

class Italian extends Lang {
  @Override
  String getHeader() {
    return "Giocatori vicini";
  }
}

class German extends Lang {
  @Override
  String getHeader() {
    return "Enge Spieler";
  }
}

class Dutch extends Lang {
  @Override
  String getHeader() {
    return "Nauwe spelers";
  }
}

class Czech extends Lang {
  @Override
  String getHeader() {
    return "Blízcí hráči";
  }
}

class Russian extends Lang {
  @Override
  String getHeader() {
    return "Близкие игроки";
  }
}

class Polish extends Lang {
  @Override
  String getHeader() {
    return "Bliscy gracze";
  }
}

class Chinese extends Lang {
  @Override
  String getHeader() {
    return "亲密玩家";
  }
}

class Japanese extends Lang {
  @Override
  String getHeader() {
    return "近いプレーヤー";
  }
}

class Korean extends Lang {
  @Override
  String getHeader() {
    return "가까운 플레이어";
  }
}

