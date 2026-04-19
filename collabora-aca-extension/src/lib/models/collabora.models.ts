/*
 * SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
 * SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13
 * SPDX-License-Identifier: Apache-2.0
 */

export interface CollaboraTokenResponse {
  access_token: string;
  access_token_ttl: string;
  wopi_src_url?: string;
}

export interface CollaboraWopiConfig {
  wopiHostUrl: string;
  wopiFileUrl: string;
  wopiSrcUrl: string;
  iFrameUrl: string;
}

export type CollaboraAction = 'edit' | 'view';
