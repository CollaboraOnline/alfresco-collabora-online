/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */
import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { map } from 'rxjs/operators';
import { CollaboraOnlineEdit, COLLABORA_EDIT, CollaboraOnlineView, COLLABORA_VIEW } from '../actions/collabora-online.actions';
import { CollaboraOnlineService } from '../../services/collabora-online.service';
import { AppService } from '@alfresco/aca-shared';

@Injectable()
export class CollaboraEffects {
  private actions$ = inject(Actions);
  private collaboraOnlineService = inject(CollaboraOnlineService);
  private appService = inject(AppService);

  collaboraOnlineEdit$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType<CollaboraOnlineEdit>(COLLABORA_EDIT),
        map((action) => {
          if (action.payload) {
            this.appService.setAppNavbarMode('collapsed');
            this.collaboraOnlineService.onEdit(action.payload);
          }
        })
      ),
    { dispatch: false }
  );
  collaboraOnlineView$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType<CollaboraOnlineView>(COLLABORA_VIEW),
        map((action) => {
          if (action.payload) {
            this.appService.setAppNavbarMode('collapsed');
            this.collaboraOnlineService.onView(action.payload);
          }
        })
      ),
    { dispatch: false }
  );
}
