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

<template>
  <div id="loolcontainer">
    <form
      id="loleafletform"
      name="loleafletform"
      method="POST"
      :action="`${collaboraUrl}WOPISrc=${wopiFileUrl}&lang=${lang}`"
      target="loleafletframe"
      hidden="hidden"
    >
      <input type="text" name="access_token" :value="accessToken" />
      <input type="text" name="access_token_ttl" :value="accessTokenTTL" />
      <input type="hidden" name="ui_defaults" :value="uiDefaults" />
      <input type="hidden" name="css_variables" :value="cssVariables" />
    </form>
    <iframe
      id="loleafletframe"
      name="loleafletframe"
      allow="fullscreen 'self' ${collaboraUrl}"
    />
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
    lang: {
      type: String,
      default: "en",
    },
    uiMode: {
      type: String,
      default: "tabbed",
    },
    cssVariables: {
      type: String,
      default: "",
      description:
        "https://sdk.collaboraonline.com/docs/theming.html#available-variables",
    },
  },
  data() {
    return {
      loading: {
        wopi: true,
        collab: true,
      },
    };
  },
  computed: {
    uiDefaults() {
      // https://sdk.collaboraonline.com/docs/theming.html
      return `UIMode=${this.uiMode}`;
    },
  },
  watch: {
    wopiFileUrl() {
      this.loading.wopi = false;
    },
    collaboraUrl() {
      this.loading.collab = false;
    },
    //Loading is watched when the component is imported in another project
    loading: {
      handler() {
        if (this.collaboraUrl && this.wopiFileUrl) {
          this.$nextTick(() => {
            this.$el.children.loleafletform.requestSubmit();
          });
        }
      },
      deep: true,
    },
  },
  //Mounted is used when the component is imported in the current project
  mounted() {
    if (this.collaboraUrl && this.wopiFileUrl) {
      this.$nextTick(() => {
        this.$el.children.loleafletform.requestSubmit();
      });
    }
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
