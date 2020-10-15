/*
 * Copyright (C) 2020 Jeci.
 * https://jeci.fr/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
export const mode = {
  "application/vnd.lotus-wordpro": "edit",
  "image/svg+xml": "edit",
  "application/vnd.ms-powerpoint": "edit",
  "application/vnd.ms-excel": "edit",
  //writer documents
  "application/vnd.sun.xml.writer": "edit",
  "application/vnd.oasis.opendocument.text": "edit",
  "application/vnd.oasis.opendocument.text-flat-xml": "edit",
  //calc documents
  "application/vnd.sun.xml.calc": "edit",
  "application/vnd.oasis.opendocument.spreadsheet": "edit",
  "application/vnd.oasis.opendocument.spreadsheet-flat-xml": "edit",
  //impress documents
  "application/vnd.sun.xml.impress": "edit",
  "application/vnd.oasis.opendocument.presentation": "edit",
  "application/vnd.oasis.opendocument.presentation-flat-xml": "edit",
  //draw documents
  "application/vnd.sun.xml.draw": "edit",
  "application/vnd.oasis.opendocument.graphics": "edit",
  "application/vnd.oasis.opendocument.graphics-flat-xml": "edit",
  //chart documents
  "application/vnd.oasis.opendocument.chart": "edit",
  //text master documents
  "application/vnd.sun.xml.writer.global": "edit",
  "application/vnd.oasis.opendocument.text-master": "edit",
  //text template documents
  "application/vnd.sun.xml.writer.template": "edit",
  "application/vnd.oasis.opendocument.text-template": "edit",
  //writer master document templates
  "application/vnd.oasis.opendocument.text-master-template": "edit",
  //spreadsheet template documents
  "application/vnd.sun.xml.calc.template": "edit",
  "application/vnd.oasis.opendocument.spreadsheet-template": "edit",
  //presentation template documents
  "application/vnd.sun.xml.impress.template": "edit",
  "application/vnd.oasis.opendocument.presentation-template": "edit",
  //drawing template documents
  "application/vnd.sun.xml.draw.template": "edit",
  "application/vnd.oasis.opendocument.graphics-template": "edit",
  //base documents
  "application/vnd.oasis.opendocument.database": "edit",
  //extensions
  "application/vnd.openofficeorg.extension": "edit",
  // microsoft word template
  "application/msword": "edit",
  // ooxml wordprocessing
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document": "edit",
  "application/vnd.ms-word.document.macroEnabled.12": "edit",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.template": "edit",
  "application/vnd.ms-word.template.macroEnabled.12": "edit",
  //ooxml spreadsheet
  "application/vnd.openxmlformats-officedocument.spreadsheetml.template": "edit",
  "application/vnd.ms-excel.template.macroEnabled.12": "edit",
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": "edit",
  "application/vnd.ms-excel.sheet.binary.macroEnabled.12": "edit",
  "application/vnd.ms-excel.sheet.macroEnabled.12": "edit",
  //ooxml presentation
  "application/vnd.openxmlformats-officedocument.presentationml.presentation": "edit",
  "application/vnd.ms-powerpoint.presentation.macroEnabled.12": "edit",
  "application/vnd.openxmlformats-officedocument.presentationml.template": "edit",
  "application/vnd.ms-powerpoint.template.macroEnabled.12": "edit",
  //others
  "application/vnd.wordperfect": "edit",
  "application/x-aportisdoc": "edit",
  "application/x-hwp": "edit",
  "application/vnd.ms-works": "edit",
  "application/x-mswrite": "edit",
  "application/x-dif-document": "edit",
  "text/spreadsheet": "edit",
  "text/csv": "edit",
  "application/x-dbase": "edit",
  "application/vnd.lotus-1-2-3": "edit",
  "image/cgm": "edit",
  "image/vnd.dxf": "edit",
  "image/x-emf": "edit",
  "image/x-wmf": "edit",
  "application/coreldraw": "edit",
  "application/vnd.visio2013": "edit",
  "application/vnd.visio": "edit",
  "application/x-mspublisher": "edit",
  "application/x-sony-bbeb": "edit",
  "application/x-gnumeric": "edit",
  "application/macwriteii": "edit",
  "application/x-iwork-numbers-sffnumbers": "edit",
  "application/vnd.oasis.opendocument.text-web": "edit",
  "application/x-pagemaker": "edit",
  "application/rtf": "edit",
  "application/x-fictionbook+xml": "edit",
  "application/clarisworks": "edit",
  "application/vnd.corel-draw": "edit",
  "image/x-wpg": "edit",
  "application/prs.plucker": "edit",
  "application/x-iwork-pages-sffpages": "edit",
  "application/vnd.openxmlformats-officedocument.presentationml.slideshow": "edit",
  "application/x-iwork-keynote-sffkey": "edit",
  "application/x-abiword": "edit",
  "image/x-freehand": "edit",
  "application/vnd.palm": "edit",
  "application/vnd.sun.xml.chart": "edit",
  "application/vnd.sun.xml.writer.web": "edit",
  "application/x-t602": "edit",
  "application/vnd.sun.xml.report.chart": "edit"
};

export function getMode(key: string): string {
  return mode[key];
}
