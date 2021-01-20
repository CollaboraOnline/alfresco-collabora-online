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
export const modeByMimetype = new Map([
  ["image/svg+xml", "view"],
  // Writer documents
  ["application/vnd.sun.xml.writer", "view"],
  ["application/vnd.oasis.opendocument.text", "edit"],
  ["application/vnd.oasis.opendocument.text-flat-xml", "edit"],
  // Calc documents
  ["application/vnd.sun.xml.calc", "view"],
  ["application/vnd.oasis.opendocument.spreadsheet", "edit"],
  ["application/vnd.oasis.opendocument.spreadsheet-flat-xml", "edit]"],
  // Impress documents
  ["application/vnd.sun.xml.impress", "view"],
  ["application/vnd.oasis.opendocument.presentation", "edit"],
  ["application/vnd.oasis.opendocument.presentation-flat-xml", "edit"],
  // Draw documents
  ["application/vnd.sun.xml.draw", "view"],
  ["application/vnd.oasis.opendocument.graphics", "view"],
  ["application/vnd.oasis.opendocument.graphics-flat-xml", "view"],
  // Chart documents
  ["application/vnd.oasis.opendocument.chart", "edit"],
  // Text master documents
  ["application/vnd.sun.xml.writer.global", "view"],
  ["application/vnd.oasis.opendocument.text-master", "edit"],
  // Math documents
  ["application/vnd.sun.xml.math", "view"],
  ["application/vnd.oasis.opendocument.formula", "edit"],
  // Text template documents
  ["application/vnd.sun.xml.writer.template", "view"],
  ["application/vnd.oasis.opendocument.text-template", "edit"],
  // Writer master document templates
  ["application/vnd.oasis.opendocument.text-master-template", "edit"],
  // Spreadsheet template documents
  ["application/vnd.sun.xml.calc.template", "view"],
  ["application/vnd.oasis.opendocument.spreadsheet-template", "edit"],
  // Presentation template documents
  ["application/vnd.sun.xml.impress.template", "view"],
  ["application/vnd.oasis.opendocument.presentation-template", "edit"],
  // Drawing template documents
  ["application/vnd.sun.xml.draw.template", "view"],
  ["application/vnd.oasis.opendocument.graphics-template", "edit"],
  // MS Word
  ["application/msword", "edit"],
  // MS Excel
  ["application/vnd.ms-excel", "edit"],
  // MS PowerPoint
  ["application/vnd.ms-powerpoint", "edit"],
  // OOXML wordprocessing
  ["application/vnd.openxmlformats-officedocument.wordprocessingml.document", "edit"],
  ["application/vnd.ms-word.document.macroEnabled.12", "edit"],
  ["application/vnd.openxmlformats-officedocument.wordprocessingml.template", "view"],
  ["application/vnd.ms-word.template.macroEnabled.12", "view"],
  // OOXML spreadsheet
  ["application/vnd.openxmlformats-officedocument.spreadsheetml.template", "view"],
  ["application/vnd.ms-excel.template.macroEnabled.12", "view"],
  ["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "edit"],
  ["application/vnd.ms-excel.sheet.binary.macroEnabled.12", "edit"],
  ["application/vnd.ms-excel.sheet.macroEnabled.12", "edit"],
  // OOXML presentation
  ["application/vnd.openxmlformats-officedocument.presentationml.presentation", "edit"],
  ["application/vnd.ms-powerpoint.presentation.macroEnabled.12", "edit"],
  ["application/vnd.openxmlformats-officedocument.presentationml.template", "edit"],
  ["application/vnd.ms-powerpoint.template.macroEnabled.12", "edit"],
  // Others
  ["application/vnd.wordperfect", "view"],
  ["application/x-aportisdoc", "view"],
  ["application/x-hwp", "view"],
  ["application/vnd.ms-works", "view"],
  ["application/x-mswrite", "view"],
  ["application/x-dif-document", "view"],
  ["text/spreadsheet", "edit"],
  ["text/csv", "edit"],
  ["application/x-dbase", "edit"],
  ["application/vnd.lotus-1-2-3", "view"],
  ["image/cgm", "view"],
  ["image/vnd.dxf", "view"],
  ["image/x-emf", "view"],
  ["image/x-wmf", "view"],
  ["application/coreldraw", "view"],
  ["application/vnd.visio2013", "view"],
  ["application/vnd.visio", "view"],
  ["application/vnd.ms-visio.drawing", "view"],
  ["application/x-mspublisher", "view"],
  ["application/x-sony-bbeb", "view"],
  ["application/x-gnumeric", "view"],
  ["application/macwriteii", "view"],
  ["application/x-iwork-numbers-sffnumbers", "view"],
  ["application/vnd.oasis.opendocument.text-web", "edit"],
  ["application/x-pagemaker", "view"],
  ["text/rtf", "edit"],
  ["text/plain", "edit"],
  ["application/x-fictionbook+xml", "view"],
  ["application/clarisworks", "view"],
  ["image/x-wpg", "view"],
  ["application/x-iwork-pages-sffpages", "view"],
  ["application/vnd.openxmlformats-officedocument.presentationml.slideshow", "edit"],
  ["application/x-iwork-keynote-sffkey", "view"],
  ["application/x-abiword", "view"],
  ["image/x-freehand", "view"],
  ["application/vnd.sun.xml.chart", "view"],
  ["application/x-t602", "view"],
  ["application/pdf", "view_comment"]
]);

export const modeByExtension = new Map([
  // Writer documents
  ["sxw", "view"],
  ["odt", "edit"],
  ["fodt", "edit"],
  // Text template documents
  ["stw", "view"],
  ["ott", "edit"],
  // MS Word
  ["doc", "edit"],
  ["dot", "edit"],
  // OOXML wordprocessing
  ["docx", "edit"],
  ["docm", "edit"],
  ["dotx", "view"],
  ["dotm", "view"],
  // Others
  ["wpd", "view"],
  ["pdb", "view"],
  ["hwp", "view"],
  ["wps", "view"],
  ["wri", "view"],
  ["lrf", "view"],
  ["mw", "view"],
  ["rtf", "edit"],
  ["txt", "edit"],
  ["fb2", "view"],
  ["cwk", "view"],
  ["pages", "view"],
  ["abw", "view"],
  ["602", "view"],
  // Text master documents
  ["sxg", "view"],
  ["odm", "edit"],
  // Writer master document templates
  ["otm", "edit"],
  ["oth", "edit"],
  // Calc documents
  ["sxc", "view"],
  ["ods", "edit"],
  ["fods", "edit"],
  // Spreadsheet template documents
  ["stc", "view"],
  ["ots", "edit"],
  // MS Excel
  ["xls", "edit"],
  ["xla", "edit"],
  // OOXML spreadsheet
  ["xltx", "view"],
  ["xltm", "view"],
  ["xlsx", "edit"],
  ["xlsb", "edit"],
  ["xlsm", "edit"],
  // Others
  ["dif", "edit"],
  ["slk", "edit"],
  ["csv", "edit"],
  ["dbf", "edit"],
  ["wk1", "view"],
  ["gnumeric", "view"],
  ["numbers", "view"],
  // Impress documents
  ["sxi", "view"],
  ["odp", "edit"],
  ["fodp", "edit"],
  // Presentation template documents
  ["sti", "view"],
  ["otp", "edit"],
  // MS PowerPoint
  ["ppt", "edit"],
  ["pot", "edit"],
  // OOXML presentation
  ["pptx", "edit"],
  ["pptm", "edit"],
  ["potx", "edit"],
  ["potm", "edit"],
  ["ppsx", "edit"],
  // Others
  ["cgm", "view"],
  ["key", "view"],
  // Draw documents
  ["sxd", "view"],
  ["odg", "view"],
  ["fodg", "view"],
  // Drawing template documents
  ["std", "view"],
  ["otg", "edit"],
  // Others
  ["svg", "view"],
  ["dxf", "view"],
  ["emf", "view"],
  ["wmf", "view"],
  ["cdr", "view"],
  ["vsd", "view"],
  ["vsdx", "view"],
  ["vss", "view"],
  ["pub", "view"],
  ["p65", "view"],
  ["wpg", "view"],
  ["fh", "view"]
]);

export function getModeByMimetype(mimetype: string): string {
  return modeByMimetype.get(mimetype);
}

export function getModeByExtension(extension: string): string {
  return modeByExtension.get(extension);
}

export function getExtensions(): string[] {
  return Array.from(modeByExtension.keys());
}
