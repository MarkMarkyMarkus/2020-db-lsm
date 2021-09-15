module db.lsm.markus {

  requires jsr305;
  requires org.slf4j;
  requires com.google.common;
  requires com.github.spotbugs.annotations;

  requires transitive org.jetbrains.annotations;

  exports ru.mail.polis;
  exports ru.mail.polis.utils;

}
