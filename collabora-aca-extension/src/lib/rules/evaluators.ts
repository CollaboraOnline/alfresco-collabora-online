/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */

import { AcaRuleContext } from '@alfresco/aca-shared/rules';

export function canUseCollaboraExtension(context: AcaRuleContext): boolean {
  if (context.navigation?.url?.startsWith('/trashcan') || !context?.appConfig.get('collabora.enable') || !context?.selection) {
    return false;
  }
  return true;
}

export function getExtension(filename: string): string {
  const position = filename.lastIndexOf('.');
  return position >= 0 ? filename.substring(position + 1).toLowerCase() : '';
}

export function canViewWithCollaboraOnline(context: AcaRuleContext): boolean {
  if (canUseCollaboraExtension(context)) {
    const file = context.selection?.file || context.selection?.first;

    if (!file?.entry) {
      return false;
    }
    const extension: string = getExtension(file.entry.name);
    const extCanView: string[] = context.appConfig.get('collabora.view');

    if (!extension) {
      return false;
    }
    return extCanView.includes(extension);
  }
  return false;
}

export function canEditWithCollaboraOnline(context: AcaRuleContext): boolean {
  if (canUseCollaboraExtension(context)) {
    const file = context.selection?.file || context.selection?.first;

    if (!file?.entry) {
      return false;
    }
    if (file.entry.isLocked) {
      return false;
    }
    if (!file.entry.properties) {
      return false;
    }
    if (file.entry.properties['cm:lockType'] === 'WRITE_LOCK' || file.entry.properties['cm:lockType'] === 'READ_ONLY_LOCK') {
      return false;
    }
    const lockOwner = file.entry.properties['cm:lockOwner'];
    if (lockOwner && lockOwner.id !== context.profile.id) {
      return false;
    }

    const extension: string = getExtension(file.entry.name);
    const extCanEdit: string[] = context.appConfig.get('collabora.edit');
    if (!extension) {
      return false;
    }
    if (extCanEdit.includes(extension)) {
      return context.permissions.check(file, ['update']);
    }
  }
  return false;
}
