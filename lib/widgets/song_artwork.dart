import 'dart:io';

import 'package:audio_service/audio_service.dart';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:musify/API/musify.dart';
import 'package:musify/widgets/no_artwork_cube.dart';
import 'package:musify/widgets/spinner.dart';

class SongArtworkWidget extends StatelessWidget {
  const SongArtworkWidget({
    super.key,
    required this.size,
    required this.metadata,
    this.borderRadius = 10.0,
    this.errorWidgetIconSize = 20.0,
  });

  final double size;
  final MediaItem metadata;
  final double borderRadius;
  final double errorWidgetIconSize;

  /// Try to determine the local file path for artwork safely.
  String? _resolveLocalArtworkPath() {
    // Prefer explicit extras key if provided by your app
    final maybeExtraPath = metadata.extras?['artWorkPath'];
    if (maybeExtraPath is String && maybeExtraPath.isNotEmpty) {
      return maybeExtraPath;
    }

    // Otherwise, if artUri is a file URI, try to extract file path
    final uri = metadata.artUri;
    if (uri != null && uri.scheme == 'file') {
      try {
        // toFilePath() will throw for non-file URIs; guard with scheme check above
        return uri.toFilePath();
      } catch (_) {
        return null;
      }
    }

    return null;
  }

  @override
  Widget build(BuildContext context) {
    final localPath = _resolveLocalArtworkPath();

    // If we have a local path and file exists -> show file image
    if (localPath != null) {
      try {
        final file = File(localPath);
        if (file.existsSync()) {
          return SizedBox(
            width: size,
            height: size,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(borderRadius),
              child: Image.file(
                file,
                fit: BoxFit.cover,
                // extra guard: show placeholder if loading fails
                errorBuilder: (ctx, error, stack) =>
                    NullArtworkWidget(iconSize: errorWidgetIconSize),
              ),
            ),
          );
        } else {
          FutureBuilder(
            future: getSongDetails(0, metadata.extras?['ytid'] ?? ''),
            builder: (context, snapshot) {
              if (!snapshot.hasData) {
                return const CircularProgressIndicator();
              }

              final newSongDetails = snapshot.data!;
              final imageUrl = newSongDetails['image'];

              return SizedBox(
                width: size,
                height: size,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(borderRadius),
                  child: CachedNetworkImage(
                    imageUrl: imageUrl,
                    fit: BoxFit.cover,
                    placeholder: (context, url) => const Spinner(),
                    errorWidget: (context, url, error) =>
                        NullArtworkWidget(iconSize: errorWidgetIconSize),
                  ),
                ),
              );
            },
          );

          // file missing on disk — log for debugging and fallthrough to placeholder
          // (Use print or your logging mechanism)
          // print('Artwork file missing: $localPath');
        }
      } catch (e) {
        // If anything unexpected happens while reading file, fall back safely
        // print('Error loading artwork file $localPath: $e');
      }
    }

    // For non-file URIs, use network image (if any) with cachedNetworkImage,
    // else fallback to placeholder
    final uri = metadata.artUri;
    if (uri != null && uri.scheme != 'file') {
      return CachedNetworkImage(
        key: ValueKey(uri.toString()),
        width: size,
        height: size,
        imageUrl: uri.toString(),
        imageBuilder: (context, imageProvider) => ClipRRect(
          borderRadius: BorderRadius.circular(borderRadius),
          child: Image(image: imageProvider, fit: BoxFit.cover),
        ),
        placeholder: (context, url) => const Spinner(),
        errorWidget: (context, url, error) =>
            NullArtworkWidget(iconSize: errorWidgetIconSize),
      );
    }

    // Nothing valid found — show placeholder
    return SizedBox(
      width: size,
      height: size,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(borderRadius),
        child: NullArtworkWidget(iconSize: errorWidgetIconSize),
      ),
    );
  }
}
