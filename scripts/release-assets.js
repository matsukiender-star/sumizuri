// Puts the files of a release straight on its GitHub release, so they never count against the storage of artifacts.
const fs = require("fs");

// Finds the release of a tag, drafts too, or makes it as a draft, and gives its id.
async function draft({ github, owner, repo, tag }) {
  const releases = await github.paginate(github.rest.repos.listReleases, { owner, repo, per_page: 100 });
  const found = releases.find((release) => release.tag_name === tag);
  if (found) return found.id;
  const made = await github.rest.repos.createRelease({ owner, repo, tag_name: tag, name: tag, draft: true });
  return made.data.id;
}

// Uploads the files in the work folder that start with the prefix, and replaces a file of the same name left by an earlier try.
async function upload({ github, owner, repo, id, prefix }) {
  const files = fs.readdirSync(".").filter((name) => name.startsWith(prefix) && fs.statSync(name).isFile());
  if (files.length === 0) throw new Error(`there are no files that start with ${prefix}`);
  const assets = await github.paginate(github.rest.repos.listReleaseAssets, { owner, repo, release_id: id, per_page: 100 });
  for (const name of files) {
    const old = assets.find((asset) => asset.name === name);
    if (old) await github.rest.repos.deleteReleaseAsset({ owner, repo, asset_id: old.id });
    await github.rest.repos.uploadReleaseAsset({
      owner,
      repo,
      release_id: id,
      name,
      data: fs.readFileSync(name),
      headers: { "content-type": "application/octet-stream", "content-length": fs.statSync(name).size },
    });
    console.log(`${name} is on the release of ${owner}/${repo}`);
  }
}

// Shows the release to everyone once every file is on it.
async function publish({ github, owner, repo, id }) {
  await github.rest.repos.updateRelease({ owner, repo, release_id: id, draft: false });
}

module.exports = { draft, upload, publish };
