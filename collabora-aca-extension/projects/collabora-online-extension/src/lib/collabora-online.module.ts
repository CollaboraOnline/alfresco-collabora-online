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

import { CollaboraOnlineService } from './collabora-online.service';
import { CollaboraOnlineEditComponent } from './collabora-online-edit.component';
import { canOpenWithCollaboraOnline } from './evaluators';
import { CollaboraEffects } from './effects/collabora-online.effects';
import { RouterModule, Routes } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { TRANSLATION_PROVIDER, ToolbarModule, PipeModule, TranslationService, ViewerModule, IconModule } from '@alfresco/adf-core';

export const COLLABORA_ROUTES: Routes = [
  { path: "collabora-online-edit/:nodeId", component: CollaboraOnlineEditComponent }
];

@NgModule({
  declarations: [CollaboraOnlineEditComponent],
  entryComponents: [CollaboraOnlineEditComponent],
  exports: [CollaboraOnlineEditComponent],
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
      'collabora-online-edit.main.component': CollaboraOnlineEditComponent
    });
    extensions.setEvaluators({
      'collabora.canOpenWithCollaboraOnline': canOpenWithCollaboraOnline
    });
    translation.addTranslationFolder(
      'assets/collabora-online-extension'
    );
  }
}
