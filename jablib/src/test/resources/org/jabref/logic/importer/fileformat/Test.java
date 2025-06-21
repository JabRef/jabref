//DEPS org.apache.tika:tika-core:3.2.0
//DEPS org.apache.tika:tika-parsers-standard-package:3.2.0

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.image.JpegParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.parser.ParseContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.File;

public class Test {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: jbang PrintMetadata.java <file>");
            System.exit(1);
        }

        String filePath = args[0];
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.err.println("File not found: " + filePath);
            System.exit(1);
        }

        try (InputStream stream = new FileInputStream(file)) {
            JpegParser parser = new JpegParser();
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler();
            ParseContext context = new ParseContext();
            context.set(Parser.class, parser);
            parser.parse(stream, handler, metadata, context);

            System.out.println("Metadata" + metadata.get(Metadata.ORIGINAL_DATE) + ":");
            for (String name : metadata.names()) {
                System.out.printf("  %s: %s%n", name, metadata.get(name));
            }
        }
    }
}

