# Adding Custom Chains Guide

## 1. Register the item

To register you chain item you need to add it to the `connectiable_chains:chainable` tag.  
This can be done by creating `data/connectiblechains/tags/items/chainable.json` in your `resources` directory.

The contents of the file should look something like this:
```json
{
  "replace": false,
  "values": [
    "examplemod:my_chain_item_id",
    "examplemod:my_other_chain_item_id"
  ]
}
```

## 2. Specify the textures

The textures of your chain are specified in a json file.
The path has to follow this convention: 
```
assets/<namespace_of_item>/textures/entity/connectible_chains_compat.json
```
Where `<namespace_of_item>` is the namespace of the item id in the `chainable` tag.  
I.e.: 
- `examplemod:my_chain_item_id` -> `examplemod`
- `examplemod_extras:chains/pink_chain` -> `examplemod_extras`

The file has to contain a `textures` object where the keys are the full item ids (same as in the `chainable` tag) 
and the value is and object with `chain` and `knot` that specify the texture ids.

For `minecraft:chain` the file is `assets/minecraft/textures/entity/connectible_chains_compat.json` and the contents are
```json
{
  "textures": {
    "minecraft:chain": {
      "chain": "minecraft:textures/block/chain",
      "knot": "connectiblechains:textures/entity/chain_knot"
    }
  }
}

```

> Note: For every namespace the texture maps get merged in order of their resource priority.

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
   The tag file **has to** be in `data/connectiblechains/tags/items/`
- If the texture is magenta and black you messed up in step 2  
   The model file **has to** be in `assets/connectiblechains/models/entity/`