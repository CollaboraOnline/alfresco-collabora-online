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
import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { map } from 'rxjs/operators';

import { CollaboraOnlineEdit, COLLABORA_EDIT } from '../actions/collabora-online.actions';
import { CollaboraOnlineService } from '../collabora-online.service';

@Injectable()
export class CollaboraEffects {
  constructor( private actions$: Actions, private collaboraOnlineService: CollaboraOnlineService) {}

  @Effect({ dispatch: false })
  collaboraOnlineEdit$ = this.actions$.pipe(
    ofType<CollaboraOnlineEdit>(COLLABORA_EDIT),
    map(action => {
      if (action.payload) {
        this.collaboraOnlineService.onEdit(action.payload);
      }
    })
  );

}
