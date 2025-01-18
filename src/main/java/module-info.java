module db.lsm.markus {

  requires jsr305;
  requires org.slf4j;
  requires com.google.common;
  requires com.github.spotbugs.annotations;
//  requires transitive jdk.incubator.foreign;

  requires transitive org.jetbrains.annotations;
    requires it.unimi.dsi.fastutil;
    requires java.sql;

    exports ru.mail.polis;

}
