[zhwanng@zhwanngdeMacBook-Pro srcs % heif-info -d /Users/zhwanng/Documents/workspace/github/heif_conformance/conformance_files/C031.heic 
MIME type: image/heif-sequence
main brand: msf1
compatible brands: msf1, hevc, iso8, mp41
Box: ftyp ----- (File Type)
size: 32   (header size: 8)
major brand: msf1
minor version: 0
compatible brands: msf1,hevc,iso8,mp41

Box: moov ----- (Movie)
size: 1351   (header size: 8)
| Box: mvhd ----- (Movie Header)
| size: 108   (header size: 12)
| version: 0
| flags: 0
| creation time:     3605693091
| modification time: 3605693091
| timescale: 1000
| duration: 800
| rate: 1
| volume: 1
| matrix:
|   1 0 0 
|   0 1 0 
|   0 0 1 
| next_track_ID: 3
| 
| Box: trak ----- (Track)
| size: 628   (header size: 8)
| | Box: tkhd ----- (Track Header)
| | size: 92   (header size: 12)
| | version: 0
| | flags: 3
| | track enabled: yes
| | track in movie: yes
| | track in preview: no
| | track size is aspect ratio: no
| | creation time:     3605693091
| | modification time: 3605693091
| | track ID: 1
| | duration: 800
| | layer: 0
| | alternate_group: 1
| | volume: 0
| | matrix:
| |   1 0 0 
| |   0 1 0 
| |   0 0 1 
| | width: 1280
| | height: 720
| | 
| | Box: mdia ----- (Media)
| | size: 528   (header size: 8)
| | | Box: mdhd ----- (Media Header)
| | | size: 32   (header size: 12)
| | | version: 0
| | | flags: 0
| | | creation time:     3605693091
| | | modification time: 3605693091
| | | timescale: 90000
| | | duration: 72000
| | | language: ```
| | | 
| | | Box: hdlr ----- (Handler Reference)
| | | size: 66   (header size: 12)
| | | pre_defined: 0
| | | handler_type: pict
| | | name: HEIF/ImageSequence/PictureHandler
| | | 
| | | Box: minf ----- (Media Information)
| | | size: 422   (header size: 8)
| | | | Box: vmhd ----- (Video Media Header)
| | | | size: 20   (header size: 12)
| | | | version: 0
| | | | flags: 1
| | | | graphics mode: 0 (copy)
| | | | op color: 0; 0; 0
| | | | 
| | | | Box: dinf ----- (Data Information)
| | | | size: 36   (header size: 8)
| | | | | Box: dref ----- (Data Reference)
| | | | | size: 28   (header size: 12)
| | | | | | Box: url  ----- (Data Entry URL)
| | | | | | size: 12   (header size: 12)
| | | | | | location: 
| | | | 
| | | | Box: stbl ----- (Sample Table)
| | | | size: 358   (header size: 8)
| | | | | Box: stsd ----- (Sample Description)
| | | | | size: 226   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0]
| | | | | | Box: hvc1 -----
| | | | | | size: 210   (header size: 8)
| | | | | | data reference index: 1
| | | | | | width: 1280
| | | | | | height: 720
| | | | | | horiz. resolution: 72
| | | | | | vert. resolution: 72
| | | | | | frame count: 1
| | | | | | compressorname: HEVC Coding
| | | | | | depth: 24
| | | | | | | Box: hvcC ----- (HEVC Configuration Item)
| | | | | | | size: 108   (header size: 8)
| | | | | | | configuration_version: 1
| | | | | | | general_profile_space: 0
| | | | | | | general_tier_flag: 0
| | | | | | | general_profile_idc: 1
| | | | | | | general_profile_compatibility_flags: 0110.0000 0000.0000 0000.0000 0000.0000 
| | | | | | | general_constraint_indicator_flags: 00000000 00000000 00000000 00000000 00000000 00000000 
| | | | | | | general_level_idc: 120
| | | | | | | min_spatial_segmentation_idc: 0
| | | | | | | parallelism_type: 0
| | | | | | | chroma_format: 4:2:0
| | | | | | | bit_depth_luma: 8
| | | | | | | bit_depth_chroma: 8
| | | | | | | avg_frame_rate: 0
| | | | | | | constant_frame_rate: 0
| | | | | | | num_temporal_layers: 1
| | | | | | | temporal_id_nested: 1
| | | | | | | length_size: 4
| | | | | | | <array>
| | | | | | | | array_completeness: 0
| | | | | | | | NAL_unit_type: 32
| | | | | | | | 40 01 0c 01 ff ff 01 60 00 00 03 00 00 03 00 00 03 00 00 03 00 78 f0 24 
| | | | | | | <array>
| | | | | | | | array_completeness: 0
| | | | | | | | NAL_unit_type: 33
| | | | | | | | 42 01 01 01 60 00 00 03 00 00 03 00 00 03 00 00 03 00 78 a0 02 80 80 2d 1f e5 f9 24 6d 9e d9 
| | | | | | | <array>
| | | | | | | | array_completeness: 0
| | | | | | | | NAL_unit_type: 34
| | | | | | | | 44 01 c1 90 95 81 12 
| | | | | | | 
| | | | | | | Box: ccst ----- (Coding Constraints)
| | | | | | | size: 16   (header size: 12)
| | | | | | | version: 0
| | | | | | | flags: 0
| | | | | | | all ref pics intra: true
| | | | | | | intra pred used: false
| | | | | | | max ref per pic: 0
| | | | | 
| | | | | Box: stts ----- (Decoding Time to Sample)
| | | | | size: 24   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0] : cnt=8, delta=9000
| | | | | 
| | | | | Box: stsc ----- (Sample to Chunk)
| | | | | size: 28   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0]
| | | | |   first chunk: 1
| | | | |   samples per chunk: 8
| | | | |   sample description index: 1
| | | | | 
| | | | | Box: stco ----- (Sample Offset)
| | | | | size: 20   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0] : 0x577
| | | | | 
| | | | | Box: stsz ----- (Sample Size)
| | | | | size: 52   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | sample count: 8
| | | | | [0] : 111554
| | | | | [1] : 111481
| | | | | [2] : 111451
| | | | | [3] : 111353
| | | | | [4] : 111362
| | | | | [5] : 111672
| | | | | [6] : 111882
| | | | | [7] : 111875
| 
| Box: trak ----- (Track)
| size: 607   (header size: 8)
| | Box: tkhd ----- (Track Header)
| | size: 92   (header size: 12)
| | version: 0
| | flags: 3
| | track enabled: yes
| | track in movie: yes
| | track in preview: no
| | track size is aspect ratio: no
| | creation time:     3605693091
| | modification time: 3605693091
| | track ID: 2
| | duration: 800
| | layer: 0
| | alternate_group: 1
| | volume: 0
| | matrix:
| |   1 0 0 
| |   0 1 0 
| |   0 0 1 
| | width: 1280
| | height: 720
| | 
| | Box: mdia ----- (Media)
| | size: 507   (header size: 8)
| | | Box: mdhd ----- (Media Header)
| | | size: 32   (header size: 12)
| | | version: 0
| | | flags: 0
| | | creation time:     3605693091
| | | modification time: 3605693091
| | | timescale: 90000
| | | duration: 72000
| | | language: ```
| | | 
| | | Box: hdlr ----- (Handler Reference)
| | | size: 61   (header size: 12)
| | | pre_defined: 0
| | | handler_type: vide
| | | name: HEIF/VideoTrack/VideoHandler
| | | 
| | | Box: minf ----- (Media Information)
| | | size: 406   (header size: 8)
| | | | Box: vmhd ----- (Video Media Header)
| | | | size: 20   (header size: 12)
| | | | version: 0
| | | | flags: 1
| | | | graphics mode: 0 (copy)
| | | | op color: 0; 0; 0
| | | | 
| | | | Box: dinf ----- (Data Information)
| | | | size: 36   (header size: 8)
| | | | | Box: dref ----- (Data Reference)
| | | | | size: 28   (header size: 12)
| | | | | | Box: url  ----- (Data Entry URL)
| | | | | | size: 12   (header size: 12)
| | | | | | location: 
| | | | 
| | | | Box: stbl ----- (Sample Table)
| | | | size: 342   (header size: 8)
| | | | | Box: stsd ----- (Sample Description)
| | | | | size: 210   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0]
| | | | | | Box: hvc1 -----
| | | | | | size: 194   (header size: 8)
| | | | | | data reference index: 1
| | | | | | width: 1280
| | | | | | height: 720
| | | | | | horiz. resolution: 72
| | | | | | vert. resolution: 72
| | | | | | frame count: 1
| | | | | | compressorname: HEVC Coding
| | | | | | depth: 24
| | | | | | | Box: hvcC ----- (HEVC Configuration Item)
| | | | | | | size: 108   (header size: 8)
| | | | | | | configuration_version: 1
| | | | | | | general_profile_space: 0
| | | | | | | general_tier_flag: 0
| | | | | | | general_profile_idc: 1
| | | | | | | general_profile_compatibility_flags: 0110.0000 0000.0000 0000.0000 0000.0000 
| | | | | | | general_constraint_indicator_flags: 00000000 00000000 00000000 00000000 00000000 00000000 
| | | | | | | general_level_idc: 120
| | | | | | | min_spatial_segmentation_idc: 0
| | | | | | | parallelism_type: 0
| | | | | | | chroma_format: 4:2:0
| | | | | | | bit_depth_luma: 8
| | | | | | | bit_depth_chroma: 8
| | | | | | | avg_frame_rate: 0
| | | | | | | constant_frame_rate: 0
| | | | | | | num_temporal_layers: 1
| | | | | | | temporal_id_nested: 1
| | | | | | | length_size: 4
| | | | | | | <array>
| | | | | | | | array_completeness: 0
| | | | | | | | NAL_unit_type: 32
| | | | | | | | 40 01 0c 01 ff ff 01 60 00 00 03 00 00 03 00 00 03 00 00 03 00 78 f0 24 
| | | | | | | <array>
| | | | | | | | array_completeness: 0
| | | | | | | | NAL_unit_type: 33
| | | | | | | | 42 01 01 01 60 00 00 03 00 00 03 00 00 03 00 00 03 00 78 a0 02 80 80 2d 1f e5 f9 24 6d 9e d9 
| | | | | | | <array>
| | | | | | | | array_completeness: 0
| | | | | | | | NAL_unit_type: 34
| | | | | | | | 44 01 c1 90 95 81 12 
| | | | | 
| | | | | Box: stts ----- (Decoding Time to Sample)
| | | | | size: 24   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0] : cnt=8, delta=9000
| | | | | 
| | | | | Box: stsc ----- (Sample to Chunk)
| | | | | size: 28   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0]
| | | | |   first chunk: 1
| | | | |   samples per chunk: 8
| | | | |   sample description index: 1
| | | | | 
| | | | | Box: stco ----- (Sample Offset)
| | | | | size: 20   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | [0] : 0x577
| | | | | 
| | | | | Box: stsz ----- (Sample Size)
| | | | | size: 52   (header size: 12)
| | | | | version: 0
| | | | | flags: 0
| | | | | sample count: 8
| | | | | [0] : 111554
| | | | | [1] : 111481
| | | | | [2] : 111451
| | | | | [3] : 111353
| | | | | [4] : 111362
| | | | | [5] : 111672
| | | | | [6] : 111882
| | | | | [7] : 111875