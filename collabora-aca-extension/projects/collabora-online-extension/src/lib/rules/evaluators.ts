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

export function canUseCollaboraExtension(context): boolean {
  if (
    context.navigation &&
    context.navigation.url &&
    context.navigation.url.startsWith("/trashcan")
  ) {
    return false;
  }

  // check if collabora is enabled
  if (!context || !context.appConfig.get('collabora.enable')) {
    return false;
  }

  // check if element is selected
  if (!context || !context.selection) {
    return false;
  }

  return true;
}

export function getExtension(filename: string): string {
  const position = filename.lastIndexOf('.');
  return filename.substring(position + 1);
}

export function canEditWithCollaboraOnline(context): boolean {
  if (canUseCollaboraExtension(context)) {
    const { file } = context.selection;

    // check if is file
    if (!file || !file.entry) {
      return false;
    }

    // check if file is locked
    if (file.entry.isLocked) {
      return false;
    }
    if (!file.entry.properties) {
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

    // check the extension of file
    const extension: string = getExtension(file.entry.name);
    const extCanEdit: string[] = context.appConfig.get('collabora.edit');
    if (!extension) {
      return false;
    }
    if (extCanEdit.indexOf(extension) > -1) {
      return context.permissions.check(file, ['update']);
    }
  }
  return false;
}
