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
import { RuleContext } from "@alfresco/adf-extensions";
import { getModeByMimetype } from "./utils";

export function canUseCollaboraExtension(context: RuleContext): boolean {
  if (
    context.navigation &&
    context.navigation.url &&
    context.navigation.url.startsWith("/trashcan")
  ) {
    return false;
  }

  if (!context || !context.selection) {
    return false;
  }
  const { file } = context.selection;

  if (!file || !file.entry) {
    return false;
  }

  if (file.entry.isLocked) {
    return false;
  }
  return true;
}

export function canEditWithCollaboraOnline(context: RuleContext): boolean {
  if (canUseCollaboraExtension(context)) {
    const { file } = context.selection;

    // check if file locked
    if (!file.entry.properties) {
      return false;
    }
    if (file.entry.isLocked) {
      return false;
    }
    if (file.entry.properties['cm:lockType'] === 'WRITE_LOCK'
        || file.entry.properties['cm:lockType'] === 'READ_ONLY_LOCK') {
      return false;
    }
    const lockOwner = file.entry.properties['cm:lockOwner'];
    if (lockOwner && lockOwner.id !== context.profile.id) {
      return false;
    }

    if (!file.entry.content.mimeType || !getModeByMimetype(file.entry.content.mimeType)) {
      return false;
    } else if (getModeByMimetype(file.entry.content.mimeType) == "edit") {
      return context.permissions.check(file, ['update']);
    } else return false;
  }
  return false;
}
