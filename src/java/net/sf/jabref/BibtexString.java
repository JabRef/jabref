/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

public class BibtexString {

    String _name, _content, _id;

    public BibtexString(String id, String name, String content) {
	_id = id;
	_name = name;
	_content = content;
    }

    public String getId() {
	return _id;
    }

    public void setId(String id) {
	_id = id;
    }

    public String getName() {
	return _name;
    }

    public void setName(String name) {
	_name = name;
    }

    public String getContent() {
	return ((_content == null) ? "" : _content);
    }

    public void setContent(String content) {
	_content = content;
    }

    public Object clone() {
      return new BibtexString(_id, _name, _content);
    }

}
