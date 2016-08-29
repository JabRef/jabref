package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

/**
 *
 * @author Usuario
 */
public class Iso690NamesAuthors implements LayoutFormatter {

    @Override
    public String format(String s) {

        if (s == null || s.trim().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        String[] authors = s.split("and");

        //parte el string en los distintos autores
        for (int i = 0; i < authors.length; i++) {
            //parte el string author en varios campos, según el separador ","
            String[] author = authors[i].split(",");

            // No separa apellidos y nombre con coma (,)
            if (author.length < 2) { // Caso 1: Nombre Apellidos
                //parte el string author en varios campos, según el separador " "
                author = authors[i].split(" ");
                //declaramos y damos un valor para evitar problemas
                String name;
                String surname;

                if (author.length == 1) {// Caso 1.0: Sólo un campo
                    sb.append(author[0].trim().toUpperCase());

                } else if (author.length == 2) {// Caso 1.1: Nombre Apellido
                    //primer campo Nombre
                    name = author[0].trim();
                    //Segundo campo Apellido
                    surname = author[1].trim().toUpperCase();

                    //añadimos los campos modificados al string final
                    sb.append(surname);
                    sb.append(", ");
                    sb.append(name);

                } else if (author.length == 3) {// Caso 1.2: Nombre Apellido1 Apellido2
                    //primer campo Nombre
                    name = author[0].trim();
                    //Segundo y tercer campo Apellido1 Apellido2
                    surname = author[1].trim().toUpperCase() + ' ' + author[2].trim().toUpperCase();

                    //añadimos los campos modificados al string final
                    sb.append(surname);
                    sb.append(", ");
                    sb.append(name);

                } else if (author.length == 4) {// Caso 1.3: Nombre SegundoNombre Apellido1 Apellido2
                    //primer y segundo campo Nombre SegundoNombre
                    name = author[0].trim() + ' ' + author[1].trim();
                    //tercer y cuarto campo Apellido1 Apellido2
                    surname = author[2].trim().toUpperCase() + ' ' + author[3].trim().toUpperCase();

                    //añadimos los campos modificados al string final
                    sb.append(surname);
                    sb.append(", ");
                    sb.append(name);
                }

            } else { // Caso 2: Apellidos, Nombre
                // Campo 1 apellidos, lo pasamos a mayusculas
                String surname = author[0].trim().toUpperCase();
                // campo 2 nombre
                String name = author[1].trim();
                //añadimos los campos modificados al string final
                sb.append(surname);
                sb.append(", ");
                sb.append(name);
            }
            if (i < authors.length - 2) { //si hay mas de 2 autores, lo separamos por ", "
                sb.append(", ");
            } else if (i == authors.length - 2) {// si hay 2 autores, lo separamos por " y "
                sb.append(" y ");
            }
        }
        return sb.toString();//retorna el string creado de autores.
    }
}
