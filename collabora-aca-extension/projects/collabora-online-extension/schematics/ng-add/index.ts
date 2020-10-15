import {
  Rule, SchematicContext, SchematicsException, Tree
} from '@angular-devkit/schematics';
import { getProjectFromWorkspace, getProjectTargetOptions } from '@angular/cdk/schematics';
import { getWorkspace } from '@schematics/angular/utility/config';
import { NodePackageInstallTask } from '@angular-devkit/schematics/tasks';

const pathSegment = '@jeci/collabora-online-extension';
const confObject = {
 "glob": "collabora-online.plugin.json",
 "input": "node_modules/@jeci/collabora-online-extension/assets",
 "output": "/assets/plugins"
};
const assetObject =
{
  "glob": "**/*",
  "input": "node_modules/@jeci/collabora-online-extension/assets",
  "output": "/assets/collabora-online-extension"
}

// Just return the tree
export function ngAdd(options: any): Rule {
  return (tree: Tree, context: SchematicContext) => {
    context.addTask(new NodePackageInstallTask());

    const workspace = getWorkspace(tree);
    const project = getProjectFromWorkspace(workspace, options.project);
    const targetOptions = getProjectTargetOptions(project, 'build');

    const workspaceConfig = tree.read('/angular.json');
    if (!workspaceConfig) {
      throw new SchematicsException('Could not find Angular workspace configuration');
    }

 if (!targetOptions.assets) {
      targetOptions.assets = [ { ...assetObject  } ];
      targetOptions.assets = [ { ...confObject  } ];
    } else {
      const assets = targetOptions.assets as Array<string | object>;
      const assetsString = JSON.stringify(assets);
      if (!assetsString.includes(pathSegment)) {
        assets.push({ ...assetObject });
        assets.push({ ...confObject });
      } else {
        console.log();
        console.log(`Could not add the icon assets to the CLI project assets ` +
          `because there is already a icon assets file referenced.`);
        console.log(`Please manually add the following config to your assets:`);
        console.log(JSON.stringify({ ...assetObject, ...confObject  }, null, 2));
        return tree;
      }
    }
    tree.overwrite('angular.json', JSON.stringify(workspace, null, 2));

    return tree;
  };
}
