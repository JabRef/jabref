ENTRY { title } {} {}
FUNCTION {article}
{ "\bibitem{" write$ cite$ write$ "}" write$ newline$
  title write$ newline$
}
FUNCTION {misc} { article }
FUNCTION {default.type} { article }
READ
ITERATE {call.type$}
