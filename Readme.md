# Polus Tiled Tiff OME Conversion Plugin

This WIPP plugin takes any image type supported by Bioformats and converts it to an OME tiled tiff. For file formats that have a pyramid like structured with multiple resolutions (or series), this plugin only saves the first series to a tiff (usually the first series is the highest resolution).

For more information on WIPP, visit the [official WIPP page](https://isg.nist.gov/deepzoomweb/software/wipp).

**This plugin is an alpha version and subject to substantial changes.**

This plugin looks for files in the metadata section of a collection that are compatible with Bioformats. In the future, WIPP will likely convert all Bioformats compatible image formats into tiled tiff, removing the need for this plugin. The current need for this plugin is that WIPPs tiled tiff conversion process only grabs the first image plane, while this plugin grabs all image planes in a series. Ultimately, this permits complete conversion of data (including all channels, z-positions, and timepoints).

## Building

To build from source code, run 
`./mvn-packager.sh`

To build the Docker image for the conversion plugin, run
`./build-docker.sh`.

## Install WIPP Plugin

If WIPP is running, navigate to the plugins page and add a new plugin. Paste the contents of `plugin.json` into the pop-up window and submit.

There is currently no publicly available docker image, so a docker image will need to be built and sent to the kubernetes cluster WIPP is running on.

## Options

This plugin takes three input arguments and one output argument:
1. An image collection
2. X tile size (width of each tile)
3. Y tile size (height of each tile)

_Note:_ All tile sizes must be a positive integer than is a multiple of 16 and smaller than 2^31.

## Run the plugin

### Run the Docker Container

```bash
docker run -v /path/to/data:/data wipp-tiledtif-converter-plugin \
  --input /data/input \
  --tileSizeX 1024 \
  --tileSizeY 1024 \
  --output /data/output
```