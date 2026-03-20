/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */
import { Component, OnInit, ViewEncapsulation, inject, DestroyRef, ViewChild, ElementRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { UserPreferencesService, UserPreferenceValues } from '@alfresco/adf-core';
import { CollaboraOnlineService } from '../../services/collabora-online.service';
import { CollaboraAction, CollaboraTokenResponse } from '../../models/collabora.models';
import { ActivatedRoute } from '@angular/router';

@Component({
  templateUrl: './collabora-base.component.html',
  styleUrls: ['./collabora-base.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class CollaboraBaseComponent implements OnInit {
  @ViewChild('form') protected postForm: ElementRef;
  @ViewChild('access_token') protected inputToken: ElementRef;
  @ViewChild('access_token_ttl') protected inputTokenTTL: ElementRef;
  @ViewChild('loleafletFrame') protected loleafletFrame: ElementRef;

  protected nodeId: string;
  protected action: CollaboraAction;
  protected accessToken: string;
  protected accessTokenTTL: string;
  protected iFrameUrl: string;
  protected locale: string;
  protected previousUrl: string;

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    protected route: ActivatedRoute,
    protected collaboraOnlineService: CollaboraOnlineService,
    protected userPreferencesService: UserPreferencesService
  ) {
    this.action = this.route.snapshot.params['action'] as CollaboraAction;
    this.nodeId = this.route.snapshot.params['nodeId'];
  }

  async ngOnInit(): Promise<void> {
    this.userPreferencesService
      .select(UserPreferenceValues.Locale)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((locale) => (this.locale = locale));

    // Get previous URL
    this.previousUrl = this.collaboraOnlineService.getPreviousUrl();

    // Get Collabora Online server URL
    const wopiHostUrl = await this.collaboraOnlineService.getLoolUrl();
    const wopiFileUrl = wopiHostUrl + 'wopi/files/' + this.nodeId;

    // Get token for document editing
    let responseToken: CollaboraTokenResponse = await this.collaboraOnlineService.getAccessToken(this.nodeId, this.action);
    this.accessToken = responseToken.access_token;
    this.accessTokenTTL = responseToken.access_token_ttl;

    if (!responseToken.wopi_src_url || responseToken.wopi_src_url === '') {
      responseToken = await this.collaboraOnlineService.getAccessToken(this.nodeId, 'edit');
    }

    const wopiSrcUrl = responseToken.wopi_src_url;
    this.iFrameUrl = wopiSrcUrl + 'WOPISrc=' + encodeURIComponent(wopiFileUrl) + '&lang=' + this.locale;

    if (this.action === 'view') {
      this.iFrameUrl += '&permission=readonly';
    }

    // Fill dynamic form
    this.postForm.nativeElement.action = this.iFrameUrl;
    this.inputToken.nativeElement.value = this.accessToken;
    this.inputTokenTTL.nativeElement.value = this.accessTokenTTL;

    // Submit the form
    this.postForm.nativeElement.submit();
  }
}
