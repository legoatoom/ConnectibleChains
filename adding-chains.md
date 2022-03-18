# Adding Custom Chains Guide

## 1. Register the item

To register you chain item you need to add it to the `connectiable_chains:chain_types` registry.  
This can be done by following these steps:

### 1.1. Add the modrinth maven to you `build.gradle`.
```groovy
repositories {
	maven {
		name = "Modrinth"
		url = "https://api.modrinth.com/maven"
		content {
			includeGroup "maven.modrinth"
		}
	}
}
```

### 1.2. Add Conectible Chains as a compile time dependency.
```groovy
dependencies {
  modCompileOnly ("maven.modrinth:connectible_chains:${project.connectible_chains_version}")
}
```

### 1.3. During your mods initialization run the following code
```java
import com.github.legoatoom.connectiblechains.chain.ChainTypesRegistry;

[...]

if(FabricLoader.getInstance().isModLoaded("connectiblechains")) {
  ChainTypesRegistry.register(myChainItem);
  ChainTypesRegistry.register(myOtherChainItem);
}
```

### 1.4. Modify your fabric.mod.json
```json
{
  "recommends": {
    "connectiblechains": ">=2.1.0"
  },
  "breaks": {
    "connectiblechains": "<2.1.0"
  }
}
```
This will ensure that the compatability does not break when an old version of connectible chains is loaed.

## 2. Specify the textures

The textures of your chain are specified in a json file.
The path has to follow this convention: 
```
assets/<item_namespace>/models/entity/chain/<item_path>.json
```
Where `<item_namespace>` is the namespace and `<item_path>` is the path of the id of the item that you registered in step 1.3..  
I.e.: 
- `examplemod:my_chain_item_id` -> `assets/examplemod/models/entity/chain/my_chain_item_id.json`
- `examplemod_extras:chains/pink_chain` -> `assets/examplemod_extras/models/entity/chain/chains/pink_chain.json`

The file has to contain a `textures` with `chain` and `knot` that specify the texture ids.

For `valley:golden_chain` the file is `assets/valley/models/entity/chain/golden_chain.json` and the contents are
```json
{
	"textures": {
		"chain": "valley:textures/blocks/golden_chain_block",
		"knot": "connectiblechains:textures/entity/golden_chain_block_knot"
	}
}
```

## 3. Create a knot texture

The chain knot (the part around the fence or wall) requires a special texture. 
You can put it wherever you want as long as the model file has the correct `chain_knot` value.
However, we recommend using the `textures/entity` directory.

You can use [src/main/resources/assets/connectiblechains/textures/entity/chain_knot.png](src/main/resources/assets/connectiblechains/textures/entity/chain_knot.png) as a template.

## 4. See if it worked

You should now be able to use the custom chain. 

### If it does not work follow these steps

- **Check the log** for errors and fix them
- If you can't place the chain then you messed up in step 1  
- If the texture is magenta and black you messed up in step 2  