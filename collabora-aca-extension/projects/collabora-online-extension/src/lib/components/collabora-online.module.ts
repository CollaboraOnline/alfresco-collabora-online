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
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EffectsModule } from '@ngrx/effects';
import { TranslateModule } from "@ngx-translate/core";

import { ExtensionService } from '@alfresco/adf-extensions';

import { CollaboraOnlineService } from '../services/collabora-online.service';
import { CollaboraOnlineComponent } from './collabora-online.component';
import { canEditWithCollaboraOnline } from '../rules/evaluators';
import { CollaboraEffects } from '../effects/collabora-online.effects';
import { RouterModule, Routes } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { TRANSLATION_PROVIDER, ToolbarModule, PipeModule, TranslationService, ViewerModule, IconModule } from '@alfresco/adf-core';

export const COLLABORA_ROUTES: Routes = [
  { path: "collabora-online/:action/:nodeId", component: CollaboraOnlineComponent }
];

@NgModule({
  declarations: [CollaboraOnlineComponent],
  entryComponents: [CollaboraOnlineComponent],
  exports: [CollaboraOnlineComponent],
  imports: [
    EffectsModule.forFeature([CollaboraEffects]),
    RouterModule.forRoot(COLLABORA_ROUTES),
    MatIconModule,
    MatToolbarModule,
    PipeModule,
    ToolbarModule,
    ViewerModule,
    CommonModule,
    IconModule,
    TranslateModule.forRoot()
  ],
  providers: [
    CollaboraOnlineService,
    {
      provide: TRANSLATION_PROVIDER,
      multi: true,
      useValue: {
        name: "collabora-online-extension",
        source: "assets/collabora-online-extension"
      }
    }
  ]
})

export class CollaboraOnlineModule {
  constructor(extensions: ExtensionService, translation: TranslationService) {
    extensions.setComponents({
      'collabora-online.main.component': CollaboraOnlineComponent
    });
    extensions.setEvaluators({
      'collabora.canEditWithCollaboraOnline': canEditWithCollaboraOnline
    });
    translation.addTranslationFolder(
      'assets/collabora-online-extension'
    );
  }
}
