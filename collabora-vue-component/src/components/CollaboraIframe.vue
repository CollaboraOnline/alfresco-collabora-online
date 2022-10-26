<!--
  Copyright (C) 2022 - Jeci SARL - https://jeci.fr

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU Affero General Public License as
  published by the Free Software Foundation, either version 3 of the
  License, or (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Affero General Public License for more details.

  You should have received a copy of the GNU Affero General Public License
  along with this program.  If not, see https://www.gnu.org/licenses/agpl-3.0.html.
-->

<!--
// Copyright (c) 2018 Ross Kaffenberger
// Copyright (c) 2022 Jérémie Lesage - Jeci
// -->
<template>
  <div id="loolcontainer">
    <form
      id="loleafletform"
      ref="collaboraForm"
      name="loleafletform"
      method="POST"
      :action="`${collaboraUrl}WOPISrc=${wopiFileUrl}`"
      target="loleafletframe"
      hidden="hidden"
    >
      <input type="text" name="access_token" :value="accessToken" />
      <input type="text" name="access_token_ttl" :value="accessTokenTTL" />
    </form>
    <iframe
      id="loleafletframe"
      name="loleafletframe"
      allow="fullscreen"
    ></iframe>
  </div>
</template>
<script>
export default {
  name: "CollaboraIframe",
  props: {
    accessToken: {
      type: String,
      default: null,
    },
    accessTokenTTL: {
      type: String,
      default: null,
    },
    wopiFileUrl: {
      type: String,
      default: null,
    },
    collaboraUrl: {
      type: String,
      default: "",
    },
  },
  data() {
    return {
      listenerHandlePostMessage: null,
      iFrameUrl: {
        type: String,
        default: null,
      },
      loading: true,
    };
  },
  watch: {
    loading(newLoadingValue) {
      if (this.wopiFileUrl !== null && newLoadingValue === false) {
        this.$refs.collaboraForm.requestSubmit();
      }
    },
  },
  mounted() {
    this.loading = false;
  },
};
</script>
<style lang="scss">
#loleafletframe {
  width: 100%;
  height: 100%;
  border: 0 none;
}

#loolcontainer {
  width: 100%;
  height: 100%;
  background: none;
}
</style>
