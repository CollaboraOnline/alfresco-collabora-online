/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */

import { Component, ViewEncapsulation, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { UserPreferencesService } from '@alfresco/adf-core';
import { CollaboraOnlineService } from '../../services/collabora-online.service';
import { CollaboraBaseComponent } from '../base/collabora-base.component';

@Component({
  selector: 'viewer-collabora-online',
  templateUrl: './viewer-collabora-online.component.html',
  styleUrls: ['./viewer-collabora-online.component.scss'],
  imports: [CommonModule],
  encapsulation: ViewEncapsulation.None
})
export class ViewerCollaboraOnlineComponent extends CollaboraBaseComponent implements OnInit {
  constructor(route: ActivatedRoute, collaboraOnlineService: CollaboraOnlineService, userPreferencesService: UserPreferencesService) {
    super(route, collaboraOnlineService, userPreferencesService);
  }

  async ngOnInit(): Promise<void> {
    return super.ngOnInit();
  }
}
