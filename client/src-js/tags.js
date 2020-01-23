var whTags = null;
const whTagGroupLimit = 16;

function resetTagsElementVisibility(tagBox) {
    let tagsElements = tagBox.querySelectorAll(".tags--top-level");
    for(let i = (tagsElements.length - 1); i >= 0; i--) {
        const hasSelectedTags = (tagsElements[i].getElementsByClassName("tag--selected").length > 0);
        if (hasSelectedTags) {
            tagsElements[i].classList.add("tags--has-selected-tags");
        } else {
            tagsElements[i].classList.remove("tags--has-selected-tags");
        }
    }
}

function resetTagVisibility(tagGroup) {
    let tags = tagGroup.querySelectorAll(".tags--unselected .tag:not(.tag--selected):not(.tag--filtered)");
    for(let i = (tags.length - 1); i >= 0; i--) {
        if(i > whTagGroupLimit) {
            tags[i].classList.add("tag--hidden");
        } else {
            tags[i].classList.remove("tag--hidden");
        }
    }
    if(tags.length === 0) {
        tagGroup.classList.add("tag-group--empty");
    } else {
        tagGroup.classList.remove("tag-group--empty");
    }
}

function resetFilteredTags(tagBox) {
    let tags = tagBox.getElementsByClassName("tag");
    let tagGroups = tagBox.querySelectorAll(".tags--unselected .tag-group");
    for(let i = (tags.length - 1); i >= 0; i--) {
        tags[i].classList.remove("tag--filtered");
    }
    for(let i = (tagGroups.length - 1); i >= 0; i--) {
        resetTagVisibility(tagGroups[i]);
    }
}

function filterTags(tagBoxId, textInput) {
    let tagBox = document.getElementById(tagBoxId);
    let tagGroups = tagBox.querySelectorAll(".tags--unselected .tag-group");
    let unselectedTags = tagBox.querySelectorAll(".tags--unselected .tag");

    if(textInput.value && textInput.value !== "") {
        let tagSearch = textInput.value.toLowerCase();

        for(let i = (unselectedTags.length - 1); i >= 0; i--) {
            let tag = unselectedTags[i];
            let tagLabel = tag.attributes["data-label"].value.toLowerCase();
            if(!tagLabel.includes(tagSearch)) {
                tag.classList.add("tag--filtered");
            } else {
                tag.classList.remove("tag--filtered");
            }
        }

        tagBox.classList.remove("tags-container--collapsed");
    } else {
        // search box is empty so remove all filtered classes
        for(let i = (unselectedTags.length - 1); i >= 0; i--) {
            unselectedTags[i].classList.remove("tag--filtered");
        }
        tagBox.classList.add("tags-container--collapsed");
    }

    // reset visibility
    for(let j = (tagGroups.length - 1); j >= 0; j--) {
        resetTagVisibility(tagGroups[j]);
    }
}

function handleTagChange(tagBox, queryParamKey) {
    if(tagBox.focusedTag) {

        // apply skeleton class to all company cards AND all tags
        let companyCards = document.getElementsByClassName("companies__company company-card");
        for(let i = (companyCards.length - 1); i >= 0; i--) {
            companyCards[i].classList.add("skeleton");
            let tags = companyCards[i].getElementsByClassName("tag");
            for(let j = (tags.length - 1); j >= 0; j--) {
                tags[j].classList.add("tag--skeleton");
            }
        }

        let tagElement = tagBox.focusedTag;
        let tagQueryId = tagBox.focusedTagQueryId;
        let adding = tagElement.classList.contains("tag--selected");
        var url = setQueryParam(null, "interaction", 1);
        url = deleteQueryParam(url, "page");
        if(tagQueryId.endsWith(":size")) {
            if(adding) {
                return setQueryParam(url, "size", tagQueryId.split(":")[0]);
            } else {
                return deleteQueryParam(url, "size");
            }
        } else {
            if(adding) {
                return addQueryParam(url, queryParamKey, tagQueryId);
            } else {
                return removeQueryParam(url, queryParamKey, tagQueryId);
            }
        }
    }
}

function createTagQueryId(slug, type) {
    return slug + ":" + type;
}

function parseSelectedTags() {
    var currentUrl = new URL(location.href);
    return currentUrl.searchParams.getAll("tag");
}

function parseSize() {
    var currentUrl = new URL(location.href);
    return currentUrl.searchParams.get("size");
}

function createIcon(icon) {
    const svgns   = "http://www.w3.org/2000/svg";
    const xlinkns = "http://www.w3.org/1999/xlink";
    let svg = document.createElementNS(svgns, "svg");
    let use = document.createElementNS(svgns, "use");
    svg.classList.add("icon");
    svg.classList.add("icon--" + icon);
    use.setAttributeNS(xlinkns, "href", "#" + icon);
    svg.appendChild(use);
    return svg;
}

function createTagGroup(parent, tagGroupType) {
    let tagGroupElement      = document.createElement("div");
    let tagGroupInnerElement = document.createElement("ul");
    tagGroupElement.classList.add("tag-group");
    tagGroupElement.classList.add("tag-group--" + tagGroupType);
    tagGroupInnerElement.classList.add("tags");
    tagGroupElement.appendChild(tagGroupInnerElement);
    parent.appendChild(tagGroupElement);
    return tagGroupInnerElement;
}

function createTag({tag, parent, grandParent, isSelected, hasIcon}) {
    let parents = grandParent.getElementsByClassName("tags");
    const tagElement = document.createElement("li");
    const tagSlugClass = "tag--slug-" + tag.slug;
    const tagTypeClass = "tag--type-" + tag.type;
    const tagQueryId = createTagQueryId(tag.slug, tag.type);
    tagElement.setAttribute("data-label", tag.label);
    tagElement.classList.add("tag");
    tagElement.classList.add(tagTypeClass);
    tagElement.classList.add(tagSlugClass);
    if(tag.subtype) {
        tagElement.classList.add("tag--subtype-" + tag.subtype)
    }
    if(isSelected) {
        tagElement.classList.add("tag--selected");
        // add a class to both parents too help with styling
        for(let i = (parents.length - 1); i>= 0; i--) {
            parents[i].classList.add("tags--has-selected-tags");
        }
    }
    if(hasIcon) {
        tagElement.appendChild(createIcon("close"));
    }
    tagElement.onclick = function() {
        let elements = grandParent.querySelectorAll("." + tagSlugClass + "." + tagTypeClass);
        for(let i = (elements.length - 1); i>= 0; i--) {
            elements[i].classList.toggle("tag--selected");
        }
        // close the expansion
        grandParent.classList.add("tags-container--collapsed");
        if(grandParent.onchange) {
            grandParent.focusedTagQueryId = tagQueryId;
            grandParent.focusedTag = tagElement;
            grandParent.onchange(tagQueryId, tagElement);
            grandParent.focusedTagQueryId = grandParent.focusedTag = null;
        }
        resetTagsElementVisibility(grandParent);
    }

    let tagSpan = document.createElement("span");
    tagSpan.innerText = tag.label;
    tagElement.appendChild(tagSpan);
    parent.appendChild(tagElement);
}

const isElementCorrect = (element, label, type) => {
    return element.innerText.toLowerCase() === label.toLowerCase()
        && element.classList.contains("tag--type-" + type.toLowerCase());
};

/**
 * Clicks on tag which includes provided label in text
 *                   & includes provided type in classlist.
 * Tag is selected from unselected tags area.
 *
 * @param {string} label
 * @param {string} type
 *
 * @example
 *     clickOnTag('Remote Working', 'benefit')
 *     clickOnTag('Scala', 'tech')
 */
const clickOnTag = (label, type) => {
    const UNSELECTED_TAGS = ".tags--unselected";
    const TAG = ".tag";

    const tagElement = Array.from(document
        .querySelector(UNSELECTED_TAGS)
        .querySelectorAll(TAG))
        .filter(elm => isElementCorrect(elm, label, type))[0];

    if (tagElement) {
        tagElement.click();
    }
};

function initTagList(tagJson) {
    whTags = (!whTags && tagJson ? JSON.parse(tagJson).tags : whTags);
}

function initTags(tagBox) {
    if(tagBox && whTags && whTags.length > 0) {
        let selectedTags = parseSelectedTags();
        let sizeValue = parseSize();
        let tagParents = [{parent:   tagBox.querySelector("ul.tags.tags--unselected"),
                           groups:   true,
                           icon:     false,
                           matchMsg: true},
                          {parent:   tagBox.querySelector("ul.tags.tags--selected"),
                           groups:   false,
                           icon:     true,
                           matchMsg: false}];
        let tagGroups = [];
        // remove any children from parent element such as loading msgs
        for (let k = (tagParents.length - 1); k >= 0; k--) {
            let tagParent    = tagParents[k].parent;
            let useGroups    = tagParents[k].groups;
            let showMatchMsg = tagParents[k].matchMsg;
            while (tagParent.firstChild) {
                tagParent.removeChild(tagParent.firstChild);
            }
            let currentGroup = (useGroups ? null : createTagGroup(tagParent, whTags[0].type));
            let currentType = null;
            for(let j = 0; j < whTags.length; j++) {
                let tag = whTags[j];
                // this group/type management code relies on tags being sorted by type
                if(useGroups && currentType != whTags[j].type) {
                    currentType = whTags[j].type;
                    currentGroup = createTagGroup(tagParent, currentType);
                    tagGroups.push(currentGroup);
                }
                createTag({tag:         tag,
                           parent:      currentGroup,
                           grandParent: tagBox,
                           isSelected:  ((-1 != selectedTags.indexOf(createTagQueryId(tag.slug, tag.type))) ||
                                         (tag.type==="size" && tag.slug===sizeValue)),
                           hasIcon:     tagParents[k].icon});
            }
            for(let j = (tagGroups.length - 1); j >= 0; j--) {
                resetTagVisibility(tagGroups[j]);
            }
            // add a text span after the tag groups
            if(showMatchMsg) {
                let noMatchingSpan = document.createElement("span");
                noMatchingSpan.innerText = "No tags matched the search term!";
                tagParent.appendChild(noMatchingSpan);
            }
        }
    }
}
