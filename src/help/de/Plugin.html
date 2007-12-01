<html xmlns="http://www.w3.org/1999/xhtml">

<body text="#275856">
    <basefont size="4"
          color="#2F4958"
          face="arial" />

    <h1>Das Plugin-System von JabRef</h1>

    <p>Ab Version 2.4 beta1 kann man JabRef mit Hilfe eines Plugin-Systems erweitern, dass
    mit dem Java Plugin Framework (JPF) erstellt wurde.</p>

    <p>Um Plugins zu nutzen, m&uuml;ssen Sie nur die jar-Datei des Plugins in einen Ordner mit dem Namen
    <code>plugins</code> speichern, wobei der <code>plugins</code>-Ordner in demselben Verzeichnis
    liegen muss, in dem sich auch die Datei JabRef.jar befindet. Beim Start von JabRef wird eine
    Liste mit allen geladenen Plugins angezeigt.</p>

    <h2>Schreiben eines Plugins</h2>

    <p>JabRef bietet die folgenden Erweiterungs-M&ouml;glichkeiten f&uuml;r Entwickler:</p>
    <ul>
      <li><code>ImportFormat</code> - Importformate hinzuf&uuml;gen, die &uuml;ber den Men&uuml;punkt <i>Datei -> Importieren in ... Datenbank</i> aufgerufen werden k&ouml;nnen.</li>
      <li><code>EntryFetcher</code> - Zugang zu Datenbanken wie Citeseer oder Medline zum <i>Internet</i>-Men&uuml; hinzuf&uuml;gen.</li>
      <li><code>ExportFormatTemplate</code> - Einen vorlagenbasierten Export wie diejenigen, die unter <i>Optionen -> Verwalte externe Exportfilter</i> verf&uuml;gbar sind, hinzuf&uuml;gen.</li>
      <li><code>ExportFormat</code> - Einen Exportfilter zum Exportdialog von JabRef hinzuf&uuml;gen&nbsp;&ndash; das ist komplizierter als einen vorlagenbasierten Export zu erstellen.</li>
      <li><code>ExportFormatProvider</code> - Ein leistungsf&auml;higerer Weg, um Exportformate hinzuzuf&uuml;gen.</li>
      <li><code>LayoutFormatter</code> - Formatierer hinzuf&uuml;gen, die im Layout-basierten Export benutzt
      werden k&ouml;nnen.</li>
	</ul>

    <p>Diese Erweiterungs-M&ouml;glichkeiten sind in <code>plugin.xml</code> des JabRef-core-plugin definiert,
    das in <code>JabRef/src/plugins/net.sf.jabref.core/</code> zu finden ist.</p>

    <p>Gehen Sie folgenderma&szlig;en vor, um ein Plugin zu erstellen:</p>
    <ol>
      <li>Machen Sie einen checkout des JabRef 'trunk' mit subversion (<code>https://jabref.svn.sourceforge.net/svnroot/jabref/trunk</code>)&nbsp;&ndash; den Ordner 'htdocs' brauchen Sie nicht.
      Im 'trunk' sind sowohl JabRef selbst als auch die Plugins enthalten, die bislang zu JabRef
      beigesteuert wurden und die einen guten Startpunkt f&uuml;r Ihre eigenen Plugins bieten.</li>
      <li>Kompilieren Sie JabRef mit <code>ant jars</code>.</li>
      <li>Erstellen Sie Ihr eigenes Projekt und definieren Sie Ihr Plugin in Ihrer eigenen plugin.xml,
      wobei Sie die oben beschriebenen Erweiterungs-M&ouml;glichkeiten beachten m&uuml;ssen.
      Achten Sie besonders darauf, dass
      <ul>
        <li>...Ihre plugin.xml einen <code>requires</code>-Bereich enth&auml;lt, der das <i>core plugin</i> (<code>net.sf.jabref.core</code>) importiert.</li>
        <li>...Ihre plugin.xml einen <code>runtime</code>-Bereich enth&auml;lt, in dem Sie JPF mitteilen,
        wo in Ihrem Projekt die class-Dateien und Ressourcen gespeichert werden.</li>
      </ul>
      </li>
      <li>Erstellen Sie eine jar-Datei Ihres Projektes und speichern es in den <code>plugins</code>-Ordner
      von JabRef.</li>
      <li>Ihr Plugin sollte nun beim Start von JabRef.jar geladen werden.</li>
    </ol>

	<p>Falls Sie noch Fragen zum Plugin-System haben, z&ouml;gern Sie nicht, sie auf der Mailing-Liste zu stellen.</p>

    <h2>Erstellen einer Erweiterungs-M&ouml;glichkeit</h2>

    <p>Dieser Abschnitt ist f&uuml;r JabRef-Entwickler gedacht, die zus&auml;tzliche Erweiterungs-M&ouml;glichkeiten
    bereitstellen wollen.</p>

    <p>Um eine neue Erweiterungs-M&ouml;glichkeit hinzuzuf&uuml;gen, m&uuml;ssen Sie diese in der plugin.xml des core-plugins
    deklarieren. Hier ist ein Beispiel:</p>

<code><pre>
&lt;extension-point id=&quot;PushToApplication&quot;&gt;
	&lt;parameter-def type=&quot;string&quot; id=&quot;pushToApp&quot;
		custom-data=&quot;&lt;classname of the interface that plugin providers need to implement&gt;&quot; /&gt;
	&lt;!-- optionally other parameters (we currently do not use any of these for anything)
		&lt;parameter-def type=&quot;string&quot; id=&quot;name&quot; /&gt;
		&lt;parameter-def type=&quot;string&quot; id=&quot;description&quot;
			multiplicity=&quot;none-or-one&quot; /&gt;
			--&gt;
&lt;/extension-point&gt;
</pre></code>

	<p>Anschlie&szlig;end m&uuml;ssen Sie den Plugin-Code-Generator "<code>ant generate</code>" aufrufen,
    der die Klasse "<code>net.sf.jabref.plugin.core.generated</code>" neu erstellt, so dass sie
    die Methode <code>getPushToApplicationExtensions()</code> enth&auml;lt; sie gibt eine Liste aller
    PushToTalk-Erweiterungen aus, die im System registriert sind.</p>

    <p>Diese Liste kann dann folgenderma&szlig;en genutzt werden (als Beispiel dient die EntryFetcher-Erweiterung):</p>

<code><pre>
/*
 * Load fetchers that are plug-in extensions
 */
JabRefPlugin jabrefPlugin = JabRefPlugin.getInstance(PluginCore.getManager());
if (jabrefPlugin != null){
	for (EntryFetcherExtension ext : jabrefPlugin.getEntryFetcherExtensions()){
		EntryFetcher fetcher = ext.getEntryFetcher();
		if (fetcher != null){
			fetchers.add(fetcher);
		}
	}
}

// and later...

for (EntryFetcher fetcher : fetchers){
  GeneralFetcher generalFetcher = new GeneralFetcher(sidePaneManager, this, fetcher);
  web.add(generalFetcher.getAction());
  fetcherActions.add(generalFetcher.getAction());
}
</pre></code>

</body>
</html>