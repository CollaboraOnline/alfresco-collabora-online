/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */

import { Action } from '@ngrx/store';
import { NodeEntry } from '@alfresco/js-api';

export const COLLABORA_EDIT = 'COLLABORA_EDIT';
export const COLLABORA_VIEW = 'COLLABORA_VIEW';

export class CollaboraOnlineEdit implements Action {
  readonly type = COLLABORA_EDIT;
  constructor(public payload: NodeEntry) {}
}
export class CollaboraOnlineView implements Action {
  readonly type = COLLABORA_VIEW;
  constructor(public payload: NodeEntry) {}
}
