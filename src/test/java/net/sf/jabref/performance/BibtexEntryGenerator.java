/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.performance;

public class BibtexEntryGenerator {

    public String generateBibtexEntries(int number) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number; i++) {
            sb.append(generateBibtexEntry(i));
            sb.append("\n");
        }
        return sb.toString();
    }

    private String generateBibtexEntry(int i) {
        return "@article{einstein1916grundlage" + i + ",\n" +
                "  title={Die grundlage der allgemeinen relativit{\\\"a}tstheorie},\n" +
                "  author={Einstein, Albert},\n" +
                "  journal={Annalen der Physik},\n" +
                "  volume={354},\n" +
                "  number={7},\n" +
                "  pages={769--822},\n" +
                "  year={1916},\n" +
                "  publisher={Wiley Online Library}\n" +
                "}\n";
    }

}
