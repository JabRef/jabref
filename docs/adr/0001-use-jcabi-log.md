# Use jcabi-log

We need to reduce startup time of JabRef (https://github.com/JabRef/jabref/issues/2966).
One time consuming aspect is the logging

## Considered Alternatives

* [jcabi-log](http://log.jcabi.com/)
* [TinyLog](http://www.tinylog.org/)

## Conclusion

* Chosen Alternative: jcabi-log (see https://github.com/JabRef/jabref/pull/3015)
* org.jabref.gui.logging.GuiAppender strongly relies on Apache Log4j2.
  TinyLog does not support mirroring log outputs and thus we cannot easily switch to TinyLog.
